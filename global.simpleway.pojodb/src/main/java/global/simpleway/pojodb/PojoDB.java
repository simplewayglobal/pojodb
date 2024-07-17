/*
 * Copyright 2018 Simpleway Holding a.s.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package global.simpleway.pojodb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.concurrent.ThreadSafe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

import global.simpleway.pojodb.format.ExternalJacksonFileFormatStrategy;
import global.simpleway.pojodb.format.IFileFormatStrategy;
import global.simpleway.pojodb.format.JsonFileFormatStrategy;
import global.simpleway.pojodb.format.YamlFileFormatStrategy;
import global.simpleway.pojodb.id.IIdGenerator;
import global.simpleway.pojodb.id.LongCounterIdGenerator;
import global.simpleway.pojodb.id.ProvidedIdGenerator;
import global.simpleway.pojodb.id.UuidIdGenerator;
import global.simpleway.pojodb.listener.Jsr308ValidatorListener;
import global.simpleway.pojodb.redoLog.RedoLogService;
import global.simpleway.pojodb.repository.AutocommitTx;
import global.simpleway.pojodb.repository.CompositeWriteCommand;
import global.simpleway.pojodb.repository.DeleteWriteCommand;
import global.simpleway.pojodb.repository.DomainRepository;
import global.simpleway.pojodb.repository.IWriteCommand;
import global.simpleway.pojodb.repository.Repository;
import global.simpleway.pojodb.repository.SaveWriteCommand;
import global.simpleway.pojodb.repository.Tx;
import global.simpleway.pojodb.repository.TxManager;
import global.simpleway.pojodb.storage.CacheDecoratorStorageBackend;
import global.simpleway.pojodb.storage.IStorageBackend;
import global.simpleway.pojodb.storage.ImmediateFlushStorageBackend;
import global.simpleway.pojodb.utils.ExceptionUtils;
import global.simpleway.pojodb.utils.LogUtil;

/**
 * Plain Old Java Objects database,
 * <p>
 * Project features:
 * <ul>
 * <li>working with java objects by default not key/values as alternatives, does not have to convert to map of map representation and then to the file</li>
 * <li>using Jackson for (un)marshalling so you could re-use your mapping from REST services or from front-end, but you could easily write another mapping</li>
 * <li>backed by file system - current implementation is using immediate flush of changes, but could use more sofisticated method of persistence (memory mapped files, in memory ...)</li>
 * <li>mandatory use of redo logs - thus your database will be always consistent. using transactions - just in auto-commit mode in current impl</li>
 * <li>(TODO) storage of binary files with theirs meta-data to be next to its binary data in the same place. Using zero copy for file serving</li>
 * <li>(TODO) could have cheap audit log through its transactions</li>
 * </ul>
 * <p>
 * Similar projects (mainly for inspiration) - MapDB and LevelDB
 *
 * @author miroslavhruz
 */
@ThreadSafe
public class PojoDB {

	private final Interner<Path> interner = Interners.newStrongInterner();

	//base path of the storage
	private final Path storagePath;

	private final TxManager txManager;

	private final RedoLogService redoLogService;

	private final IFileFormatStrategy defaultFileFormat;
	private final IFileFormatStrategy redoLogFileFormat;

	private final IIdGenerator<?, ?> defaultIdGenerator;
	private final IIdGenerator<Object, String> redoLogIdGenerator;

	private final boolean enableValidation;

	private final Map<Class<?>, Repository<?, ?>> repositories = new HashMap<>();

	private PojoDB(Path storagePath, IFileFormatStrategy defaultFileFormat, IFileFormatStrategy redoLogFileFormat, IIdGenerator<?, ?> defaultIdGenerator,
			IIdGenerator<Object, String> redoLogIdGenerator, boolean enableValidation) {

		this.storagePath = storagePath;
		this.defaultFileFormat = defaultFileFormat;
		this.redoLogFileFormat = redoLogFileFormat;
		this.defaultIdGenerator = defaultIdGenerator;
		this.redoLogIdGenerator = redoLogIdGenerator;

		this.redoLogService = new RedoLogService(this, redoLogFileFormat, redoLogIdGenerator);
		this.txManager = new TxManager();

		this.enableValidation = enableValidation;
	}

	public Object intern(Path itemLevelLock) {
		return interner.intern(itemLevelLock);
	}

	public static PojoDBBuilder builder() {
		return new PojoDBBuilder();
	}

	public Path getStoragePath() {
		return storagePath;
	}

	public RedoLogService getRedoLogService() {
		return redoLogService;
	}

	public TxManager getTxManager() {
		return txManager;
	}

	/*package*/ IIdGenerator<?, ?> getDefaultIdGenerator() {
		return defaultIdGenerator;
	}

	/*package*/ IIdGenerator<Object, String> getRedoLogIdGenerator() {
		return redoLogIdGenerator;
	}

	/*package*/ IFileFormatStrategy getDefaultFileFormat() {
		return defaultFileFormat;
	}

	/*package*/ IFileFormatStrategy getRedoLogFileFormat() {
		return redoLogFileFormat;
	}

	public <T extends IDomainObject> DomainRepository<T> newCachedDomainRepository(String collectionName, Class<T> typeClass) {
		final ImmediateFlushStorageBackend<String> backend = new ImmediateFlushStorageBackend<>(this, collectionName, defaultFileFormat, s -> s);
		final CacheDecoratorStorageBackend<String> cachedBackend = new CacheDecoratorStorageBackend<>(backend, typeClass);
		@SuppressWarnings("unchecked") final IIdGenerator<T, String> idGenerator = (IIdGenerator<T, String>) defaultIdGenerator;

		return newDomainRepository(typeClass, idGenerator, cachedBackend);
	}

	public <T extends IDomainObject> DomainRepository<T> newDomainRepository(String collectionName, Class<T> typeClass) {
		@SuppressWarnings("unchecked") final IIdGenerator<T, String> idGenerator = (IIdGenerator<T, String>) defaultIdGenerator;

		return newDomainRepository(typeClass, idGenerator, new ImmediateFlushStorageBackend<>(this, collectionName, defaultFileFormat, s -> s));
	}

	public <T extends IDomainObject> DomainRepository<T> newDomainRepository(Class<T> typeClass, IIdGenerator<T, String> idGenerator, IStorageBackend<String> storageStrategy) {
		final DomainRepository<T> repository = new DomainRepository<>(typeClass, this, storageStrategy, idGenerator);

		if (enableValidation) {
			repository.addListener(new Jsr308ValidatorListener<>());
		}

		if (repositories.containsKey(typeClass)) throw new IllegalArgumentException(LogUtil.build("Repository with same class: {} already exists.", typeClass));
		synchronized (repositories) {
			repositories.put(typeClass, repository);
		}

		return repository;
	}

	public <T, ID> Repository<T, ID> newCachedRepository(String collectionName, Class<T> typeClass, Function<T, ID> idSupplier, BiConsumer<T, ID> idConsumer,
			Function<String, ID> idCreator) {
		final ImmediateFlushStorageBackend<ID> backend = new ImmediateFlushStorageBackend<>(this, collectionName, defaultFileFormat, idCreator);
		final CacheDecoratorStorageBackend<ID> cachedBackend = new CacheDecoratorStorageBackend<>(backend, typeClass);
		@SuppressWarnings("unchecked")
		final IIdGenerator<T, ID> idGenerator = (IIdGenerator<T, ID>) defaultIdGenerator;
		
		return newRepository(typeClass, idSupplier, idConsumer, idGenerator, cachedBackend);
	}

	public <T, ID> Repository<T, ID> newRepository(String collectionName, Class<T> typeClass, Function<T, ID> idSupplier, BiConsumer<T, ID> idConsumer,
			Function<String, ID> idCreator) {
		@SuppressWarnings("unchecked")
		final IIdGenerator<T, ID> idGenerator = (IIdGenerator<T, ID>) defaultIdGenerator;
		
		return newRepository(typeClass, idSupplier, idConsumer, idGenerator, new ImmediateFlushStorageBackend<>(this, collectionName, defaultFileFormat, idCreator));
	}

	public <T, ID> Repository<T, ID> newRepository(Class<T> typeClass, Function<T, ID> idSupplier, BiConsumer<T, ID> idConsumer, IIdGenerator<T, ID> idGenerator,
			IStorageBackend<ID> storageStrategy) {
		final Repository<T, ID> repository = new Repository<>(typeClass, idSupplier, idConsumer, this, storageStrategy, idGenerator);

		if (enableValidation) {
			repository.addListener(new Jsr308ValidatorListener<>());
		}

		if (repositories.containsKey(typeClass)) throw new IllegalArgumentException(LogUtil.build("Repository with same class: {} already exists.", typeClass));
		synchronized (repositories) {
			repositories.put(typeClass, repository);
		}

		return repository;
	}

	public void deregisterRepository(Class<?> clazz) {
		synchronized (repositories) {
			if (repositories.containsKey(clazz) == false) throw new IllegalArgumentException(LogUtil.build("Repository with class: {} not registered.", clazz));

			repositories.remove(clazz);
		}
	}

	public Repository<?, ?> getRepository(Class<?> clazz) {
		return repositories.get(clazz);
	}

	public Collection<Repository<?, ?>> getRepositories() {
		return Collections.unmodifiableCollection(repositories.values());
	}

	public Tx newTx() {
		return txManager.newTx(this);
	}

	public boolean checkForConsistencyAndRepair() {
		synchronized (repositories) {
			Preconditions.checkArgument(getRepositories().isEmpty() == false, "Could not check for consistency and repair with no repositories :-(");

			final Map<String, IWriteCommand> allIncompleteTxs = getRedoLogService().getAllIncompleteTxs();
			//this is not important which type exactly the repo is but need any.
			final Repository<?, ?> anyRepository = getRepositories().iterator().next();

			ExceptionUtils.iterateAllAndThrowIfAnyException(() -> LogUtil.build("Could not check for consistency and repair db"), allIncompleteTxs.keySet(), opId -> {

				final IWriteCommand writeCommand = allIncompleteTxs.get(opId);

				// FIXME: 02/10/2017 hacky find repo now
				if (writeCommand instanceof SaveWriteCommand<?, ?>) {
					final SaveWriteCommand<?, ?> saveWriteCommand = (SaveWriteCommand<?, ?>) writeCommand;
					final Object item = saveWriteCommand.getItem();
					final Repository<?, ?> repository = getRepository(item.getClass());

					try (AutocommitTx<?, ?> autocommitTx = new AutocommitTx<>(repository, writeCommand)) {
						autocommitTx.skipBegin(opId);
						autocommitTx.commitOrRollback();
					} catch (IOException e) {
						throw new IllegalArgumentException(e);
					}
				} else if (writeCommand instanceof DeleteWriteCommand<?, ?>) {
					final DeleteWriteCommand<?, ?> deleteWriteCommand = (DeleteWriteCommand<?, ?>) writeCommand;
					final Class<?> itemClass = deleteWriteCommand.getItemClass();
					final Repository<?, ?> repository = getRepository(itemClass);

					try (AutocommitTx<?, ?> autocommitTx = new AutocommitTx<>(repository, writeCommand)) {
						autocommitTx.skipBegin(opId);
						autocommitTx.commitOrRollback();
					} catch (IOException e) {
						throw new IllegalArgumentException(e);
					}

				} else if (writeCommand instanceof CompositeWriteCommand<?>) {
					Tx tx = null;
					try {
						tx = newTx();

						tx.skipBegin(opId);
						tx.withCommand(anyRepository, writeCommand);
						tx.commitOrRollback();
					} catch (IOException e) {
						throw new IllegalStateException(e);
					} finally {
						if (tx != null) {
							tx.close();
						}
					}
				} else {
					throw new IllegalStateException("Unknown write command: " + writeCommand);
				}
			});
		}

		return false;
	}

	public static class PojoDBBuilder {

		private PojoDBBuilder() {
			//no code
		}

		private Path storagePath = Paths.get(".");

		private IFileFormatStrategy fileFormat = new JsonFileFormatStrategy();

		private IIdGenerator<?, ?> idGenerator = new LongCounterIdGenerator<>();
		private IIdGenerator<Object, String> redoLogIdGenerator = new LongCounterIdGenerator<>();

		private boolean enableValidation = true;

		public PojoDBBuilder withPath(Path storagePath) {
			this.storagePath = storagePath;
			return this;
		}

		public PojoDBBuilder withPath(String storagePath) {
			this.storagePath = Paths.get(storagePath);
			return this;
		}

		public PojoDBBuilder withPathInCWD() {
			return withPath(".");
		}

		public PojoDBBuilder withPathInTmpFolder() {
			try {
				final Path tempDirectory = Files.createTempDirectory("pojodb");

				tempDirectory.toFile().deleteOnExit();

				return withPath(tempDirectory);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}

		public PojoDBBuilder withCustomFileFormat(IFileFormatStrategy fileFormat) {
			this.fileFormat = fileFormat;
			return this;
		}

		public PojoDBBuilder withJsonFileFormat() {
			this.fileFormat = new JsonFileFormatStrategy();
			return this;
		}

		public PojoDBBuilder withExternalJacksonFileFormat(ObjectMapper jackson) {
			this.fileFormat = new ExternalJacksonFileFormatStrategy(jackson);
			return this;
		}

		public PojoDBBuilder withYamlFileFormat() {
			this.fileFormat = new YamlFileFormatStrategy();
			return this;
		}

		public PojoDBBuilder withLongCounterIdGenerator() {
			this.idGenerator = new LongCounterIdGenerator<>();
			this.redoLogIdGenerator = new LongCounterIdGenerator<>();
			return this;
		}

		public PojoDBBuilder withUuidIdGenerator() {
			this.idGenerator = new UuidIdGenerator<>();
			this.redoLogIdGenerator = new UuidIdGenerator<>();
			return this;
		}

		public PojoDBBuilder withProvidedIdGenerator() {
			this.idGenerator = new ProvidedIdGenerator<>();
			this.redoLogIdGenerator = new ProvidedIdGenerator<>();
			return this;
		}

		public PojoDBBuilder enableValidation() {
			this.enableValidation = true;
			return this;
		}

		public PojoDBBuilder disableValidation() {
			this.enableValidation = false;
			return this;
		}

		public PojoDB build() {
            return new PojoDB(storagePath, fileFormat, fileFormat, idGenerator, redoLogIdGenerator, enableValidation);
		}

	}

}

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

package global.simpleway.pojodb.repository;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import global.simpleway.pojodb.listener.IPojoDBListener;
import global.simpleway.pojodb.redoLog.RedoLogService;
import global.simpleway.pojodb.storage.IStorageBackend;
import global.simpleway.pojodb.utils.LogUtil;
import global.simpleway.pojodb.PojoDB;
import global.simpleway.pojodb.id.IIdGenerator;

public class Repository<T, ID> {

	private static final Logger logger = LoggerFactory.getLogger(Repository.class);

	private final PojoDB pojoDB;

	private final IIdGenerator<T, ID> idGenerator;
	// getter of the id
	private final Function<T, ID> idSupplier;
	// setter of the id
	private final BiConsumer<T, ID> idConsumer;
	private final IStorageBackend<ID> backend;

	private final Class<T> typeClass;

	private final List<IPojoDBListener<T>> listeners = new CopyOnWriteArrayList<>();

	public Repository(Class<T> typeClass, Function<T, ID> idSupplier, BiConsumer<T, ID> idConsumer, PojoDB storage, IStorageBackend<ID> storageStrategy,
			IIdGenerator<T, ID> idGenerator) {

		this.typeClass = typeClass;
		this.pojoDB = storage;
		this.backend = storageStrategy;
		this.idGenerator = idGenerator;
		this.idSupplier = idSupplier;
		this.idConsumer = idConsumer;

		idGenerator.setLastId(getLastId());
	}

	public RedoLogService getRedoLogService() {
		return pojoDB.getRedoLogService();
	}

	public PojoDB getPojoDB() {
		return pojoDB;
	}

	public Tx newTx() {
		return getPojoDB().newTx();
	}

	public TxManager getTxManager() {
		return getPojoDB().getTxManager();
	}

	@VisibleForTesting
	public IIdGenerator<T, ID> getIdGenerator() {
		return idGenerator;
	}

	@VisibleForTesting
	public IStorageBackend<ID> getBackend() {
		return backend;
	}

	public Class<T> getTypeClass() {
		return typeClass;
	}

	private ID getLastId() {
		final List<ID> allIds = findAll().stream()
				.map(idSupplier)
				.sorted()
				.collect(Collectors.toList());

		if (allIds == null || allIds.isEmpty()) return null;
		return allIds.get(allIds.size() - 1);
	}

	public boolean addListener(IPojoDBListener<T> listener) {
		return listeners.add(listener);
	}

	public boolean removeListener(IPojoDBListener<T> listener) {
		return listeners.remove(listener);
	}

	public List<T> findAll() {
		return backend.findAllIds()
				.stream()
				.map(this::findOne)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public List<T> findAllById(List<ID> ids) {
		Preconditions.checkArgument(ids != null, "The given Iterable of entities not be null!");

		return backend.findAllIds()
				.stream()
				.filter(ids::contains)
				.map(this::findOne)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public <C> List<C> findAllByClass(Class<C> clazz) {
		Preconditions.checkArgument(clazz != null, "Class could not be null!");

		return (List<C>) findAll().stream()
				.filter(i -> clazz.isAssignableFrom(i.getClass()))
				.collect(Collectors.toList());
	}

	public T findOne(ID id) {
		Preconditions.checkArgument(id != null, "The given id must not be null!");

		final boolean exists = backend.exists(id);
		if (!exists) {
			return null;
		}

		try {
			// FIXME: 03/10/2017 on before convert, on after convert
			final T value = (T) backend.read(id, getTypeClass());

			return value;
		} catch (IOException e1) {
			logger.warn("Could not read item: {}", id, e1);
			return null;
		}
	}

	public <S extends T> List<S> saveAll(Iterable<S> items) {
		Preconditions.checkArgument(items != null, "The given Iterable of entities not be null!");

		final List<S> list = StreamSupport.stream(items.spliterator(), false)
				.collect(Collectors.toList());

		//save empty list is doing nothing, it's not an error
		if (list.isEmpty()) return list;

		//0. generates new ids if possible
		items.forEach(this::beforeSave);

		tx(SaveListWriteCommand.create(list, getTypeClass(), idSupplier));

		return list;
	}

	/**
	 * Save one item to the database
	 *
	 * @param item
	 * @return
	 */
	public <S extends T> S save(S item) {

		//0. generates new id if possible
		beforeSave(item);

		final SaveWriteCommand<T, ID> writeCommand = new SaveWriteCommand<>(item, idSupplier);

		tx(writeCommand);

		return item;
	}

	private <S extends T> void beforeSave(S item) {
		Preconditions.checkArgument(item != null, "Entity must not be null!");

		//generate new id when we are not using provided id generator 
		if (isTransient(item)) {
			idGenerator.setNextId(item, idConsumer);
		}

		listeners.forEach(l -> l.onBeforeSave(item));
	}

	public void deleteAll() {

		//yes this is not ideal for performance, but you want to use #deleteAll() on production? :-) 
		deleteAll(findAll());
	}

	/**
	 * Delete items from database
	 *
	 * @param items
	 */
	public void deleteAll(List<T> items) {
		Preconditions.checkArgument(items != null, "The given Iterable of entities not be null!");

		//delete empty list is doing nothing, it's not an error
		if (items.isEmpty()) {
			return;
		}

		boolean containsAtLeastOneTransient = items.stream()
				.filter(this::isTransient)
				.findAny()
				.orElse(null) != null;

		Preconditions.checkArgument(containsAtLeastOneTransient == false, "Could not delete transient item");

		items.forEach(i -> {
			listeners.forEach(l -> l.onBeforeDelete(i));
		});

		tx(DeleteListWriteCommand.create(items, getTypeClass(), idSupplier));
	}

	/**
	 * Delete one item from database
	 *
	 * @param item
	 */
	public void delete(T item) {
		Preconditions.checkArgument(item != null, "The given Iterable of entities not be null!");
		Preconditions.checkArgument(isTransient(item) == false, LogUtil.build("Could not delete transient item: {}", item));
		Preconditions.checkArgument(isExistingOrInCurrentTx(idSupplier.apply(item)), LogUtil.build("Could not delete not existing item with: {}", item));

		listeners.forEach(l -> l.onBeforeDelete(item));

		tx(new DeleteWriteCommand<>(idSupplier.apply(item), item.getClass()));
	}

	private boolean isExistingOrInCurrentTx(ID id) {
		final T existing = findOne(id);
		final boolean isCreateInCurrentTx = getTxManager().getCurrentTx() != null ? getTxManager().getCurrentTx().isAnyCreate(id) : false;

		return existing != null || isCreateInCurrentTx;
	}

	public void deleteById(ID id) {
		Preconditions.checkArgument(id != null, "The given id must not be null!");
		Preconditions.checkArgument(isExistingOrInCurrentTx(id), LogUtil.build("Could not delete not existing item with: {}", id));

		tx(new DeleteWriteCommand<>(id, getTypeClass()));
	}

	public boolean existsById(ID id) {
		Preconditions.checkArgument(id != null, "The given id must not be null!");

		return backend.exists(id);
	}

	public long count() {
		return findAll().size();
	}

	/*package*/ void rollback_single(ASingleIdWriteCommand<?, ?> writeCommand, TxContext context) throws IOException {
		context.setTypeClass(getTypeClass());

		backend.rollback((ID) writeCommand.getId(), context);
	}

	/*package*/ void backup_single(ASingleIdWriteCommand<?, ?> writeCommand, TxContext context) throws IOException {
		context.setTypeClass(getTypeClass());

		backend.backup((ID) writeCommand.getId(), context);
	}

	/*package*/ void clearBackup_single(ASingleIdWriteCommand<?, ?> writeCommand, TxContext context) throws IOException {
		context.setTypeClass(getTypeClass());

		backend.clearBackup((ID) writeCommand.getId(), context);
	}

	/*package*/ void commit_save(SaveWriteCommand<?, ?> saveWriteCommand, TxContext context) throws IOException {
		backend.save((ID) saveWriteCommand.getId(), backend.getFileFormat().toString(saveWriteCommand.getItem()), context);
	}

	/*package*/ void commit_delete(DeleteWriteCommand<?, ?> deleteWriteCommand, TxContext context) throws IOException {
		backend.delete((ID) deleteWriteCommand.getId(), context);
	}

	private void tx(IWriteCommand writeCommand) {
		if (pojoDB.getTxManager().isInAutocommit()) {

			//execute now
			try (AutocommitTx<T, ID> tx = new AutocommitTx<>(this, writeCommand)) {
				tx.commitOrRollback();
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}

		} else {

			//add to the current tx
			pojoDB.getTxManager()
					.getCurrentTx()
					.withCommand(this, writeCommand);
		}
	}

	private boolean isTransient(T item) {
		return item != null && idSupplier.apply(item) == null;
	}

}

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

package global.simpleway.pojodb.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import global.simpleway.pojodb.PojoDB;
import global.simpleway.pojodb.format.IFileFormatStrategy;
import global.simpleway.pojodb.repository.NioFileUtils;
import global.simpleway.pojodb.repository.TxContext;

/**
 * Each atomic operation consists of 1 file system file and its flushed to the disk immediately in executing thread
 * <p>
 * <ul>
 * <li>one operation (even composite tx) has 1 file</li>
 * <li>one item in collection</li>
 * <li>one index</li>
 * </ul>
 */
public class ImmediateFlushStorageBackend<ID> implements IStorageBackend<ID> {

	private static final Logger logger = LoggerFactory.getLogger(ImmediateFlushStorageBackend.class);

	private final PojoDB pojoDB;

	//name of the collection as folder
	private final String collectionName;

	//file format of the collection
	private final IFileFormatStrategy fileFormatStrategy;

	//map of file ids to who ever are waiting for tx to complete
	private final Map<String, Integer> currentTxCount = new HashMap<>();
	//many reads of the map and only one write to the map
	private final ReadWriteLock currentTxCountLock = new ReentrantReadWriteLock();
	
	private final Function<String, ID> idCreator;

	public ImmediateFlushStorageBackend(PojoDB pojoDB, String collectionName, IFileFormatStrategy fileFormatStrategy, Function<String, ID> idCreator) {
		this.pojoDB = pojoDB;
		this.collectionName = collectionName;
		this.fileFormatStrategy = fileFormatStrategy;
		this.idCreator = idCreator;
		
		createRepositoryPath();
	}

	private Path getRepositoryPath() {
		return pojoDB.getStoragePath().resolve(collectionName);
	}

	private void createRepositoryPath() {
		try {
			Files.createDirectories(getRepositoryPath());
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Production file which is holding POJO data
	 *
	 * @param id
	 * @return
	 */
	private Path getFilePathByString(String id) {
		return getRepositoryPath().resolve(id + fileFormatStrategy.getFileExtension());
	}

	private Path getFilePath(ID id) {
		return getFilePathByString("" + id);
	}

	/**
	 * Old file acts as backup of production file, when there are multiple outgoing concurrent operations of the same file,
	 * it's backed up only for first time
	 *
	 * @param id
	 * @return
	 */
	private Path getOldFilePathByString(String id) {
		return Paths.get(getFilePathByString(id).toAbsolutePath().toString() + ".old");
	}

	private Path getOldFilePath(ID id) {
		return getOldFilePathByString("" + id);
	}

	/**
	 * New file should exists in synchronized (id) lock just during flushing content to be written to the some file
	 * New file should immediately be renamed to desired file
	 *
	 * @param id
	 * @return
	 */
	private Path getNewFilePath(ID id) {
		return Paths.get(getFilePath(id).toAbsolutePath().toString() + ".new");
	}

	@Override
	public Object read(ID id, Class<?> clazz) throws IOException {
		String data = null;

		final Path path = getFilePath(id);

		synchronized (pojoDB.intern(path)) {
			//does not belong to any current tx
			if (isInCurrentTx(id) == false) {
				data = NioFileUtils.read(path);
			}
			//ok so it's somewhere between backup() and clearBackup()
			else {
					data = NioFileUtils.read(getOldFilePath(id));
			}
		}

		return getFileFormat().fromString(data, clazz);
	}

	@Override
	public void delete(ID id, TxContext context) throws IOException {
		final Path path = getFilePath(id);

		synchronized (pojoDB.intern(path)) {
			NioFileUtils.delete(path);
		}
	}

	@Override
	public IFileFormatStrategy getFileFormat() {
		return fileFormatStrategy;
	}

	@Override
	public void rollback(ID id, TxContext context) throws IOException {
		final Path original = getFilePath(id);
		final Path backup = getOldFilePath(id);

		synchronized (pojoDB.intern(original)) {
			final boolean originalExists = NioFileUtils.exists(original);
			final boolean backupExists = NioFileUtils.exists(backup);

			//do not harm .old file, do whatever you want with original when there are more concurrent TXs

			if (backupExists == false && originalExists == false) {
				logger.warn("Backup {} nor original {} not exists, have nothing to rollback.", backup, original);
				return;
			}
			//rollback from empty backup means correct rollback of CREATE
			else if (backupExists == false) {
				NioFileUtils.delete(original);
			}
			//rollback from backup to existing original means correct rollback of UPDATE
			//rollback from backup to empty original means correct rollback of DELETE
			else if (backupExists) {
				final Path newFilePath = getNewFilePath(id);

				//atomic operation, read will always read good in newFilePath
				NioFileUtils.copy(backup, newFilePath);
				NioFileUtils.rename(newFilePath, original);
			}
		}

	}

	@Override
	public Set<ID> findAllIds() {
		//return all files in collection folder
		//so there could be even uncommitted changes, .old and .new files
		//and from them we want only committed data
		final List<File> allFiles = Arrays.asList(getRepositoryPath().toFile().listFiles());

		return allFiles.stream()
				.map(file -> {
					final Path path = file.toPath();

					if (!NioFileUtils.exists(path)) return null;

					final String fileName = file.getName();
					final String id = getBaseNameWithoutAnyExtension(fileName);

					//ok, it not belongs to any outgoing transaction
					if (isInCurrentTxByString(id) == false) return id;

					//uncommitted CREATE => .new exists, .old does not exists and file could exists with value or not
					//uncommitted UPDATE => .new exists, .old exists and file exists with old or new value
					//uncommitted DELETE => .new does not exists, .old exists and file could exists with old value or not

					//so safest way is to look for .old file, so it will filter uncommitted CREATEs and DELETEs (we are listing even .old and .new files)

					final Path oldFilePath = getOldFilePathByString(id);
					if (allFiles.contains(oldFilePath.toFile())) {
						return id;
					}
					return null;
				})
				.filter(Objects::nonNull)
				.sorted()
				.map(idCreator)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Override
	public void backup(ID id, TxContext txContext) throws IOException {
		final Path original = getFilePath(id);
		final Path backup = getOldFilePath(id);

		synchronized (pojoDB.intern(original)) {
			final boolean exists = NioFileUtils.exists(original);
			if (!exists) {
				logger.trace("File {} not exists, have nothing to backup.", original);
			}

			//copy from prod to .old only for first current tx, other will have it already there and 
			//what is worse could backup (and even rollback) partial committed file which is very very bad :-)
			if (exists && isInCurrentTx(id) == false) {
				final Path newFilePath = getNewFilePath(id);

				NioFileUtils.copy(original, newFilePath);
				NioFileUtils.rename(newFilePath, backup);
			}

			//here I know that there is .old file and I could rely on it
			incrementTxCount(id);
		}
	}

	private void incrementTxCountByString(String id) {
		currentTxCountLock.writeLock().lock();
		try {
			if (currentTxCount.containsKey(id)) {
				Integer count = currentTxCount.get(id);
				if (count == null) throw new IllegalStateException("Could not read tx count");
				currentTxCount.put(id, ++count);
			} else {
				currentTxCount.put(id, 1);
			}
		} finally {
			currentTxCountLock.writeLock().unlock();
		}
	}

	private void incrementTxCount(ID id) {
		incrementTxCountByString("" + id);
	}

	private boolean decrementTxCountByString(String id) {
		currentTxCountLock.writeLock().lock();
		try {
			if (currentTxCount.containsKey(id)) {
				Integer count = currentTxCount.get(id);
				if (count == null) throw new IllegalStateException("Could not read tx count");
				else if (count == 1) {
					currentTxCount.remove(id);
					return true;
				} else {
					currentTxCount.put(id, --count);
					return false;
				}
			} else {
				return false;
			}
		} finally {
			currentTxCountLock.writeLock().unlock();
		}
	}

	private boolean decrementTxCount(ID id) {
		return decrementTxCountByString("" + id);
	}

	private boolean isInCurrentTx(ID id) {
		return isInCurrentTxByString("" + id);
	}

	private boolean isInCurrentTxByString(String id) {
		currentTxCountLock.readLock().lock();
		try {
			return currentTxCount.containsKey(id);
		} finally {
			currentTxCountLock.readLock().unlock();
		}
	}

	@Override
	public void clearBackup(ID id, TxContext context) throws IOException {
		final Path original = getFilePath(id);
		final Path backup = getOldFilePath(id);

		synchronized (pojoDB.intern(original)) {
			final boolean exists = NioFileUtils.exists(backup);
			if (!exists) {
				logger.trace("File {} not exists, have nothing to clear.", backup);
			}

			//delete backup file if I am last one who is using it in current tx list
			if (decrementTxCount(id)) {
				//decrement counter even if file does not exists
				if (exists) {
					NioFileUtils.delete(backup);
				}
			}
		}
	}

	private String getBaseNameWithoutAnyExtension(String fileName) {
		if (fileName == null) return null;

		final String[] split = StringUtils.split(fileName, ".");
		if (split == null || split.length == 0) return null;

		return split[0];
	}

	@Override
	public void save(ID id, String data, TxContext context) throws IOException {
		final Path path = getFilePath(id);
		final Path newPath = getNewFilePath(id);

		synchronized (pojoDB.intern(path)) {

			//this is very tricky part :-)

			//OS filesystem implementation specific
			//but on UNIX and on new Windows 7 and above is rename of file atomic operation
			//so write next to the file to the .new file handle, flush the buffer, do your stuff here
			//and when you are done, rename .new file to the original file, so reader will have always good variant

			//and about current tx counter, there is no need to synchronize 2 threads from both saving to .new
			//because of this synchronized block

			NioFileUtils.save(newPath, data);
			NioFileUtils.rename(newPath, path);
		}
	}

	@Override
	public boolean exists(ID id) {
		final Path path = getFilePath(id);

		//there is not any outgoing tx for this file
		if (isInCurrentTx(id) == false) {
			return NioFileUtils.exists(path);
		}

		//there is some outgoing operation in progress so prod file could be rollbacked or exists if op is CREATE
		//so safest way how to read uncommitted data is to test .old file
		synchronized (pojoDB.intern(path)) {
			final Path oldFilePath = getOldFilePath(id);
			return NioFileUtils.exists(oldFilePath);
		}
	}

}

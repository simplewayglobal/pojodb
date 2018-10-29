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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import global.simpleway.pojodb.format.IFileFormatStrategy;
import global.simpleway.pojodb.repository.TxContext;

/**
 * All READs are cached, all UPDATEs, CREATEs and DELETEs are changing cache on the fly.
 */
public class CacheDecoratorStorageBackend<ID> implements IStorageBackend<ID> {

	private static final Logger logger = LoggerFactory.getLogger(CacheDecoratorStorageBackend.class);

	private final IStorageBackend<ID> delegate;

	//caching real objects, it's embedded database, so evict could not fetch all data from db.
	//instead of it changing data when committing to the file system
	private final ConcurrentMap<ID, Object> committedCache = new ConcurrentHashMap<>();

	//this is better way how to do it then on file system
	//there are separated committed and uncommitted data for some opId
	private final Map<String, Map<ID, String>> currentTxCache = new HashMap<>();

	public CacheDecoratorStorageBackend(IStorageBackend<ID> delegate, Class<?> typeClass) {
		this.delegate = delegate;

		initialize(typeClass);
	}

	private void initialize(Class<?> typeClass) {
		delegate.findAllIds().forEach(id -> {
			try {
				final Object pojo = delegate.read(id, typeClass);
				committedCache.put(id, pojo);
			} catch (IOException e) {
				logger.warn("Could not read stored object for id: {}", id, e);
			}
		});
	}

	@Override
	public Set<ID> findAllIds() {
		return committedCache.keySet();
	}

	@Override
	public void backup(ID id, TxContext txContext) throws IOException {
		delegate.backup(id, txContext);
	}

	@Override
	public void clearBackup(ID id, TxContext context) throws IOException {
		delegate.clearBackup(id, context);

		//here we could commit the rest in copy on write cache after cleaning .old files
		doCommit(id, context);
	}

	@Override
	public void save(ID id, String data, TxContext context) throws IOException {
		delegate.save(id, data, context);

		doSaveOrDelete(id, data, context);
	}

	@Override
	public boolean exists(ID id) {
		return committedCache.containsKey(id);
	}

	@Override
	public Object read(ID id, Class<?> clazz) throws IOException {
		return committedCache.get(id);
	}

	@Override
	public void delete(ID id, TxContext context) throws IOException {
		delegate.delete(id, context);

		doSaveOrDelete(id, null, context);
	}

	@Override
	public IFileFormatStrategy getFileFormat() {
		return delegate.getFileFormat();
	}

	@Override
	public void rollback(ID id, TxContext context) throws IOException {
		doRollback(id, context);

		delegate.rollback(id, context);
	}

	private void doSaveOrDelete(ID id, String data, TxContext context) {
		synchronized (currentTxCache) {
			final String opId = context.getOpId();

			currentTxCache.putIfAbsent(opId, new HashMap<>());

			final Map<ID, String> scope = currentTxCache.get(opId);
			scope.put(id, data);

			currentTxCache.put(opId, scope);
		}
	}

	private void doRollback(ID id, TxContext context) {
		synchronized (currentTxCache) {
			final String opId = context.getOpId();

			final Map<ID, String> scope = currentTxCache.get(opId);
			if (scope == null) return;

			scope.remove(id);

			if (scope.isEmpty()) {
				currentTxCache.remove(opId);
			} else {
				currentTxCache.put(opId, scope);
			}
		}
	}

	private void doCommit(ID id, TxContext context) {
		synchronized (currentTxCache) {
			final Map<ID, String> scope = currentTxCache.get(context.getOpId());

			//commit do nothing in case of rollback
			if (scope == null || scope.containsKey(id) == false) return;

			final String value = scope.remove(id);

			if (value != null) {
				try {
					final Object pojo = delegate.read(id, context.getTypeClass());
					committedCache.put(id, pojo);
				} catch (IOException e) {
					throw new IllegalArgumentException(e);
				}
			} else {
				committedCache.remove(id);
			}

			if (scope.isEmpty()) {
				currentTxCache.remove(context.getOpId());
			}
		}
	}
}
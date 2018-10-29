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

import global.simpleway.pojodb.storage.IStorageBackend;
import global.simpleway.pojodb.IDomainObject;
import global.simpleway.pojodb.PojoDB;
import global.simpleway.pojodb.id.IIdGenerator;

public class DomainRepository<T extends IDomainObject> extends Repository<T, String> {
	
	public DomainRepository(Class<T> typeClass, PojoDB storage, IStorageBackend<String> storageStrategy, IIdGenerator<T, String> idGenerator) {
		super(typeClass, IDomainObject::getId, IDomainObject::setId, storage, storageStrategy, idGenerator);
	}
}

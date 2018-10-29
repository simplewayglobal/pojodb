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
import java.util.Set;

import global.simpleway.pojodb.format.IFileFormatStrategy;
import global.simpleway.pojodb.repository.TxContext;

public interface IStorageBackend<ID> {

	public Set<ID> findAllIds();

	public void backup(ID id, TxContext txContext) throws IOException;
	
	public void clearBackup(ID id, TxContext context) throws IOException;
	
	public void save(ID id, String data, TxContext context) throws IOException;

	public boolean exists(ID id);
	
	public Object read(ID id, Class<?> clazz) throws IOException;
	
	public void delete(ID id, TxContext context) throws IOException;
	
	public IFileFormatStrategy getFileFormat();
	
	void rollback(ID id, TxContext context) throws IOException;

}

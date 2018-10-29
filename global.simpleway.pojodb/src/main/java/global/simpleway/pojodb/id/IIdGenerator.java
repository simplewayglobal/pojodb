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

package global.simpleway.pojodb.id;

import java.util.function.BiConsumer;

/**
 * Constructs id to use for object to be persistent
 */
public interface IIdGenerator<T, ID> {

	/**
	 * Marks last used id in space to be potentially incremented
	 *
	 * @param id
	 */
	public void setLastId(ID id);

	/**
	 * Computes next id
	 *
	 * @return
	 */
	public ID getNextId();

	/**
	 * Sets nextId to pojo
	 *
	 * @param pojo
	 */
	void setNextId(T pojo, BiConsumer<T, ID> setIdConsumer);
}

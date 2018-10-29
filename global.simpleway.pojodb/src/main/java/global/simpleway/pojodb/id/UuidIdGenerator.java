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

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Generates IDs as random unique ids
 */
public class UuidIdGenerator<T> implements IIdGenerator<T,String> {
	
	@Override
	public void setLastId(String id) {
		//no code	
	}

	@Override
	public String getNextId() {
		return (String) UUID.randomUUID().toString();
	}

	@Override
	public void setNextId(T pojo, BiConsumer<T,String> setIdConsumer) {
		if (pojo != null) {
			setIdConsumer.accept(pojo, getNextId());
		}
	}
}

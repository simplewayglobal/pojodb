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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates Ids as sequence from 1 and higher per collection space
 * 
 */
public class LongCounterIdGenerator<T> implements IIdGenerator<T,String> {

	private static final Logger logger = LoggerFactory.getLogger(LongCounterIdGenerator.class);

	private final AtomicLong counter = new AtomicLong();
	
	private final Object writeLock = new Object();

	@Override
	public void setLastId(String id) {
		synchronized (writeLock) {
			long lastId = 0;

			if (id != null) {
				try {
					lastId = Long.valueOf(id);
				} catch (NumberFormatException e) {
					logger.warn("Could not parse [{}] to long, skipping", id);
				}
			}

			counter.set(lastId);
		}
	}

	@Override
	public String getNextId() {
		return "" + counter.incrementAndGet();
	}

	@Override
	public void setNextId(T pojo, BiConsumer<T,String> setIdConsumer) {
		if (pojo != null) {
			setIdConsumer.accept(pojo, getNextId());
		}
	}
}

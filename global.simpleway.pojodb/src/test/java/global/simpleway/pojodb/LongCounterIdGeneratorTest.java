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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import global.simpleway.pojodb.id.LongCounterIdGenerator;

public class LongCounterIdGeneratorTest {

	@Test
	public void nextId() {
		final LongCounterIdGenerator<String> generator = new LongCounterIdGenerator<>();

		assertThat(generator.getNextId()).isEqualTo("1");
		assertThat(generator.getNextId()).isEqualTo("2");
		assertThat(generator.getNextId()).isEqualTo("3");
	}

	@Test
	public void setLastId() {
		final LongCounterIdGenerator<String> generator = new LongCounterIdGenerator<>();

		generator.setLastId("678");

		assertThat(generator.getNextId()).isEqualTo("679");
		assertThat(generator.getNextId()).isEqualTo("680");
		assertThat(generator.getNextId()).isEqualTo("681");
	}

	@Test
	public void setLastId_null() {
		final LongCounterIdGenerator<String> generator = new LongCounterIdGenerator<>();

		generator.setLastId(null);

		assertThat(generator.getNextId()).isEqualTo("1");
		assertThat(generator.getNextId()).isEqualTo("2");
		assertThat(generator.getNextId()).isEqualTo("3");
	}

	@Test
	public void overflow() {
		final LongCounterIdGenerator<String> generator = new LongCounterIdGenerator<>();

		generator.setLastId("" + (Long.MAX_VALUE - 1));

		assertThat(generator.getNextId()).isEqualTo("" + Long.MAX_VALUE);
		assertThat(generator.getNextId()).isEqualTo("" + Long.MIN_VALUE);
		assertThat(generator.getNextId()).isEqualTo("" + (Long.MIN_VALUE + 1));
	}
}

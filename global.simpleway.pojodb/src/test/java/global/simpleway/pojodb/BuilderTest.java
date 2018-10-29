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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import global.simpleway.pojodb.bo.QueueItem;
import global.simpleway.pojodb.bo.User;
import global.simpleway.pojodb.format.IFileFormatStrategy;
import global.simpleway.pojodb.format.JsonFileFormatStrategy;
import global.simpleway.pojodb.format.YamlFileFormatStrategy;
import global.simpleway.pojodb.id.LongCounterIdGenerator;
import global.simpleway.pojodb.id.ProvidedIdGenerator;
import global.simpleway.pojodb.id.UuidIdGenerator;
import global.simpleway.pojodb.repository.DomainRepository;
import global.simpleway.pojodb.repository.TxContext;
import global.simpleway.pojodb.storage.IStorageBackend;
import global.simpleway.pojodb.storage.ImmediateFlushStorageBackend;

public class BuilderTest {

	@Test
	public void defaultValues() {
		final PojoDB pojoDB = PojoDB.builder().build();

		assertThat(pojoDB.getStoragePath()).isEqualTo(Paths.get("."));

		assertThat(pojoDB.getDefaultIdGenerator().getClass()).isEqualTo(LongCounterIdGenerator.class);
		assertThat(pojoDB.getRedoLogIdGenerator().getClass()).isEqualTo(LongCounterIdGenerator.class);

		assertThat(pojoDB.getDefaultFileFormat().getClass()).isEqualTo(JsonFileFormatStrategy.class);
		assertThat(pojoDB.getRedoLogFileFormat().getClass()).isEqualTo(JsonFileFormatStrategy.class);
	}

	@Test
	public void differentRepositoriesHasDifferentStorageAndIdStrategy() {
		final PojoDB pojoDB = PojoDB.builder().build();

		final UuidIdGenerator<User> uuidIdGenerator = new UuidIdGenerator<>();
		final DummyBackend dummyBackend = new DummyBackend();
		final DomainRepository<User> userRepository = pojoDB.newDomainRepository(User.class, uuidIdGenerator, dummyBackend);

		final ProvidedIdGenerator<QueueItem, String> providedIdGenerator = new ProvidedIdGenerator<>();
		final ImmediateFlushStorageBackend<String> backend = new ImmediateFlushStorageBackend<>(pojoDB, "queueItem", new YamlFileFormatStrategy(), s -> s);
		final DomainRepository<QueueItem> queueItemRepository = pojoDB.newDomainRepository(QueueItem.class, providedIdGenerator, backend);

		assertThat(userRepository.getIdGenerator()).isSameAs(uuidIdGenerator);
		assertThat(userRepository.getBackend()).isSameAs(dummyBackend);
		assertThat(queueItemRepository.getIdGenerator()).isSameAs(providedIdGenerator);
		assertThat(queueItemRepository.getBackend()).isSameAs(backend);
	}

	// FIXME: 27/09/2017 test cleanup of directory in test case
}

class DummyBackend implements IStorageBackend<String> {

	@Override
	public Set<String> findAllIds() {
		return Collections.emptySet();
	}

	@Override
	public void backup(String id, TxContext txContext) throws IOException {

	}

	@Override
	public void clearBackup(String id, TxContext context) throws IOException {

	}

	@Override
	public void save(String id, String data, TxContext context) throws IOException {

	}

	@Override
	public boolean exists(String id) {
		return false;
	}

	@Override
	public Object read(String id, Class<?> clazz) throws IOException {
		return null;
	}

	@Override
	public void delete(String id, TxContext context) throws IOException {

	}

	@Override
	public IFileFormatStrategy getFileFormat() {
		return null;
	}

	@Override
	public void rollback(String id, TxContext context) throws IOException {

	}

}

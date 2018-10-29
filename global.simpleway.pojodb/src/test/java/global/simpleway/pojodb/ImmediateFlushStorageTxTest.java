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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import global.simpleway.pojodb.format.JsonFileFormatStrategy;
import global.simpleway.pojodb.id.LongCounterIdGenerator;
import global.simpleway.pojodb.repository.DomainRepository;
import global.simpleway.pojodb.repository.Repository;
import global.simpleway.pojodb.repository.Tx;

public class ImmediateFlushStorageTxTest extends ATxTest {

	private DomainRepository<Pojo> repository;

	@Before
	public void before() {
		final PojoDB pojoDB = PojoDB.builder().withPathInTmpFolder().build();
		repository = pojoDB.newDomainRepository("pojo", Pojo.class);
	}

	@After
	public void after() {
		FileUtils.deleteQuietly(repository.getPojoDB().getStoragePath().toFile());
	}

	@Override
	protected Repository<Pojo, String> repository() {
		return repository;
	}

	//all tests are from superclass

	@Test
	public void commit_rollback_dueToError() throws IOException {
		final PojoDB pojoDB = PojoDB.builder().withPathInTmpFolder().build();
		//setup custom buggy file provider which someday throws exception
		final BuggyFileProvider fileProvider = new BuggyFileProvider(pojoDB, "pojo", new JsonFileFormatStrategy());
		repository = pojoDB.newDomainRepository(Pojo.class, new LongCounterIdGenerator<>(), fileProvider);

		final Pojo pojo = create(1, "1");
		repository().save(pojo);

		Tx tx = null;
		try {
			tx = repository().newTx();

			pojo.setIntValue(2);
			pojo.setStringValue("2");
			repository().save(pojo);

			repository().save(create(3, "3"));
			repository().save(create(4, "4"));


			fileProvider.activateHellMachine();

			try {
				tx.commit();

				Assert.fail("Should throw IOException and go to catch");
			} catch (IOException e) {

				fileProvider.pleaseStahp();

				//we should do the manual rollback
				tx.rollback();

				//current tx shoul be roll backed so there must be only 1 item
				assertThat(repository().count()).isOne();

				//and that item should be in previous state
				final Pojo loaded = repository().findOne(pojo.getId());
				assertThat(loaded.getIntValue()).isEqualTo(1);
				assertThat(loaded.getStringValue()).isEqualTo("1");
			}
		}
		finally {
			if (tx != null) {
				tx.close();
			}
		}
	}
}

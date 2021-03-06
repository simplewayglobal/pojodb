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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import global.simpleway.pojodb.repository.DomainRepository;

public class ImmediateFlushStorageBasicCrudTest extends ABasicCrudTest {

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
	DomainRepository<Pojo> repository() {
		return repository;
	}

	//all test are from super class
}

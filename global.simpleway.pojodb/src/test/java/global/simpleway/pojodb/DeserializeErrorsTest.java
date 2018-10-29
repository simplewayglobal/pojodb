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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import global.simpleway.pojodb.repository.DomainRepository;

public class DeserializeErrorsTest {

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

	private Pojo create(Integer intValue, String stringValue) {
		final Pojo pojo = new Pojo();
		pojo.setIntValue(intValue);
		pojo.setStringValue(stringValue);
		return pojo;
	}
	
	private void classNotExists(String id) throws IOException {
		final Path filePath = repository.getPojoDB().getStoragePath().resolve("pojo").resolve(id + ".json");

		final String wrongContent = Files.readAllLines(filePath).stream()
				.map(l -> {
					return l.replaceAll("\"class\":\"cz.sw.pojodb.Pojo\"", "\"class\":\"NonexistingClass\"");
				})
				.collect(Collectors.joining());

		FileUtils.writeStringToFile(filePath.toFile(),wrongContent, StandardCharsets.UTF_8);
	}

	private void wrongContent(String id) throws IOException {
		final Path filePath = repository.getPojoDB().getStoragePath().resolve("pojo").resolve(id + ".json");

		final String wrongContent = "It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using 'Content here, content here', making it look like readable English. Many desktop publishing packages and web page editors now use Lorem Ipsum as their default model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like).";
		FileUtils.writeStringToFile(filePath.toFile(),wrongContent, StandardCharsets.UTF_8);
	}

	@Test
	public void findAll_loadNotExistingClass() throws IOException {
		final Pojo pojo = create(1, "1");
		repository.save(pojo);
		
		classNotExists(pojo.getId());
		
		Assertions.assertThat(repository.findAll()).isEmpty();
	}


	@Test
	public void findAll_wrongContent() throws IOException {
		final Pojo pojo = create(1, "1");
		repository.save(pojo);

		wrongContent(pojo.getId());

		Assertions.assertThat(repository.findAll()).isEmpty();
	}



	@Test
	public void findOne_loadNotExistingClass() throws IOException {
		final Pojo pojo = create(1, "1");
		repository.save(pojo);

		classNotExists(pojo.getId());
		
		assertThat(repository.findOne(pojo.getId())).isNull();
	}

	@Test
	public void findOne_wrongContent() throws IOException {
		final Pojo pojo = create(1, "1");
		repository.save(pojo);

		wrongContent(pojo.getId());

		assertThat(repository.findOne(pojo.getId())).isNull();
	}


	@Test
	public void findAllById_loadNotExistingClass() throws IOException {
		final Pojo pojo = create(1, "1");
		repository.save(pojo);

		classNotExists(pojo.getId());

		Assertions.assertThat(repository.findAllById(Arrays.asList(pojo.getId()))).isEmpty();
	}


	@Test
	public void findAllById_wrongContent() throws IOException {
		final Pojo pojo = create(1, "1");
		repository.save(pojo);

		wrongContent(pojo.getId());

		Assertions.assertThat(repository.findAllById(Arrays.asList(pojo.getId()))).isEmpty();
	}

	@Test
	public void count_loadNotExistingClass() throws IOException {
		final Pojo pojo = create(1, "1");
		repository.save(pojo);

		classNotExists(pojo.getId());

		assertThat(repository.count()).isZero();
	}


	@Test
	public void count_wrongContent() throws IOException {
		final Pojo pojo = create(1, "1");
		repository.save(pojo);

		wrongContent(pojo.getId());

		assertThat(repository.count()).isZero();
	}

}

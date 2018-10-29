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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import global.simpleway.pojodb.repository.DomainRepository;

public class ParentChildRepositoryTest {

	private DomainRepository<Parent> repository;

	@Before
	public void before() {
		final PojoDB pojoDB = PojoDB.builder().withPathInTmpFolder().build();
		repository = pojoDB.newDomainRepository("parent", Parent.class);
	}

	@After
	public void after() {
		FileUtils.deleteQuietly(repository.getPojoDB().getStoragePath().toFile());
	}

	@Test
	public void saveAndLoad() {
		final Child child = new Child("1", "1", "1");
		repository.save(child);

		final Parent parent = new Parent("2", "2");
		repository.save(parent);

		assertThat(repository.findAll()).containsOnly(child, parent);
	}

	@Test
	public void findByClass() {
		final Child c1 = new Child("1", "c1", "c1");
		final Child c2 = new Child("2", "c2", "c2");
		repository.saveAll(Arrays.asList(c1, c2));

		final Parent m = new Parent("3", "m");
		final Parent f = new Parent("4", "f");
		repository.saveAll(Arrays.asList(m, f));

		final List<Child> children = repository.findAllByClass(Child.class);
		assertThat(children).containsOnly(c1, c2);

		final List<Parent> persons = repository.findAllByClass(Parent.class);
		assertThat(persons).containsOnly(c1, c2, m, f);
	}

}

class Parent extends AJacksonDomainObject {

	private String id;

	private String value;

	@JsonCreator
	public Parent(@JsonProperty("id") String id, @JsonProperty("value") String value) {
		this.id = id;
		this.value = value;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Parent)) return false;
		final Parent parent = (Parent) o;
		return Objects.equals(getId(), parent.getId()) && // NOSONAR 
				Objects.equals(getValue(), parent.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getValue());
	}
}

class Child extends Parent {

	private String opinion;

	@JsonCreator
	public Child(@JsonProperty("id") String id, @JsonProperty("value") String value, @JsonProperty("opinion") String opinion) {
		super(id, value);
		this.opinion = opinion;
	}

	public String getOpinion() {
		return opinion;
	}

	public void setOpinion(String opinion) {
		this.opinion = opinion;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Child)) return false;
		if (!super.equals(o)) return false;
		final Child child = (Child) o;
		return Objects.equals(getOpinion(), child.getOpinion());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getOpinion());
	}
}
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
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import global.simpleway.pojodb.repository.Repository;

public abstract class ABasicCrudTest {

	protected Pojo create(Integer intValue, String stringValue) {
		final Pojo pojo = new Pojo();
		pojo.setIntValue(intValue);
		pojo.setStringValue(stringValue);
		return pojo;
	}
	
	abstract Repository<Pojo, String> repository();

	@Test
	public void findAll() {
		Assertions.assertThat(repository().findAll()).isEmpty();

		final Pojo pojo = create(23, "el pojo loco");

		repository().save(pojo);

		Assertions.assertThat(repository().findAll()).isNotEmpty();
		Assertions.assertThat(repository().findAll()).containsExactly(pojo);
	}

	@Test
	public void findOne() {
		final Pojo p1 = create(1, "1");
		p1.setId("1");

		final Pojo p2 = create(2, "2");
		p2.setId("2");

		repository().save(p1);
		repository().save(p2);

		final Pojo l1 = repository().findOne(p1.getId());
		final Pojo l2 = repository().findOne(p2.getId());

		assertThat(l1).isNotNull().isEqualTo(p1);
		assertThat(l2).isNotNull().isEqualTo(p2);
	}

	@Test
	public void findOne_notExisting() {
		final Pojo pojo = repository().findOne("not existing");

		assertThat(pojo).isNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void findOne_null() {
		repository().findOne(null);
	}

	@Test
	public void findAllById() {
		final Pojo p1 = create(1, "1");
		final Pojo p2 = create(2, "2");
		final Pojo p3 = create(3, "3");

		repository().saveAll(Arrays.asList(p1, p2, p3));

		final List<Pojo> allById = repository().findAllById(Arrays.asList(p3.getId(), p2.getId()));

		Assertions.assertThat(allById).hasSize(2);

		//yes it's sorted by id and not by your input
		assertThat(allById.get(0)).isEqualTo(p2);
		assertThat(allById.get(1)).isEqualTo(p3);
	}

	@Test
	public void save() {
		final Pojo p1 = create(23, "El pojo loco");
		repository().save(p1);
		final Pojo p2 = create(24, "Twin sister");
		repository().save(p2);

		Assertions.assertThat(repository().findAll()).containsOnly(p1, p2);
	}

	@Test
	public void saveAndLoad_mustBeSameObject() {
		final Pojo original = create(1, "Test");
		final Pojo detached = original.clone();
		assertThat(detached.getId()).isNull();

		final Pojo saved = repository().save(original);
		assertThat(saved).isSameAs(original);

		final Pojo loaded = repository().findOne(saved.getId());
		assertThat(loaded).isNotSameAs(saved);
		assertThat(loaded).isEqualTo(saved);

		assertThat(loaded.getId()).isEqualTo(saved.getId());
	}

	@Test
	public void saveAll() {
		final Pojo p1 = create(1, "1");
		final Pojo p2 = create(2, "2");

		repository().saveAll(Arrays.asList(p1, p2));
		Assertions.assertThat(repository().findAll()).containsOnly(p1, p2);
	}

	@Test
	public void saveAll_emptyListDoNothing() {
		repository().saveAll(Collections.emptyList());
	}

	@Test
	public void count() {
		assertThat(repository().count()).isEqualTo(0);

		final Pojo p1 = create(1, "1");
		repository().save(p1);
		assertThat(repository().count()).isEqualTo(1);

		repository().save(create(2, "2"));
		assertThat(repository().count()).isEqualTo(2);

		repository().deleteById(p1.getId());
		assertThat(repository().count()).isEqualTo(1);
	}

	@Test
	public void delete() {
		final Pojo p1 = create(1, "1");
		final Pojo p2 = create(2, "2");

		repository().saveAll(Arrays.asList(p1, p2));

		repository().delete(p2);
		Assertions.assertThat(repository().findAll()).containsOnly(p1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void delete_nullItem() {
		repository().delete(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void delete_transientItem() {
		repository().delete(create(1, "1"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteById_nullId() {
		repository().deleteById(null);
	}

	@Test
	public void deleteById() {
		final Pojo p1 = create(1, "1");
		final Pojo p2 = create(2, "2");

		repository().saveAll(Arrays.asList(p1, p2));

		repository().deleteById(p2.getId());
		Assertions.assertThat(repository().findAll()).containsOnly(p1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteById_notExistingId() {
		repository().deleteById("not existing");
	}

	@Test
	public void existsById() {
		final Pojo p1 = create(1, "1");
		repository().save(p1);

		final Pojo p2 = create(2, "2");
		repository().save(p2);

		assertThat(repository().existsById(p1.getId())).isTrue();
		assertThat(repository().existsById("not existing")).isFalse();
	}


	@Test
	public void deleteAll() {
		final Pojo p1 = create(1, "1");
		final Pojo p2 = create(2, "2");

		repository().saveAll(Arrays.asList(p1, p2));

		repository().deleteAll();
		assertThat(repository().count()).isZero();
	}
}

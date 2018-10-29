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
import java.util.Arrays;

import org.junit.Test;

import global.simpleway.pojodb.format.IFileFormatStrategy;
import global.simpleway.pojodb.repository.Repository;
import global.simpleway.pojodb.repository.Tx;
import global.simpleway.pojodb.repository.TxContext;
import global.simpleway.pojodb.storage.ImmediateFlushStorageBackend;

public abstract class ATxTest {
	
	protected Pojo create(Integer intValue, String stringValue) {
		final Pojo pojo = new Pojo();
		pojo.setIntValue(intValue);
		pojo.setStringValue(stringValue);
		return pojo;
	}
	
	protected abstract Repository<Pojo,String> repository();
	
	@Test
	public void autocommit_changesAreCommitedAtOnce() {
		assertThat(repository().count()).isZero();

		repository().save(create(1, "1"));
		
		//changes are committed at once
		assertThat(repository().count()).isOne();
	
		//there is not an active transaction
		assertThat(repository().getTxManager().getCurrentTx()).isNull();
	}

	@Test
	public void commit() throws IOException {

		final Pojo p1 = create(1, "1");
		p1.setId("1");
		repository().save(p1);

		Tx tx = null;
		try {
			tx = repository().newTx();
			repository().delete(p1);
			tx.commit();

		} finally {
			if (tx != null) {
				tx.close();
			}
		}
	}

	@Test
	public void objectsCreatedInSameTxAreVisibleWithEachOther_delete() throws IOException {
		Tx tx = null;
		try {
			tx = repository().newTx();

			final Pojo p1 = create(1, "1");
			p1.setId("1");
			repository().save(p1);
			repository().delete(p1);

			tx.commit();

		} finally {
			if (tx != null) {
				tx.close();
			}
		}
	}

	@Test
	public void objectsCreatedInSameTxAreVisibleWithEachOther_deleteById() throws IOException {
		Tx tx = null;
		try {
			tx = repository().newTx();

			final Pojo p1 = create(1, "1");
			p1.setId("1");
			repository().save(p1);
			repository().deleteById(p1.getId());

			tx.commit();

		} finally {
			if (tx != null) {
				tx.close();
			}
		}
	}

	@Test
	public void objectsCreatedInSameTxAreVisibleWithEachOther_deleteAll() throws IOException {
		Tx tx = null;
		try {
			tx = repository().newTx();

			final Pojo p1 = create(1, "1");
			p1.setId("1");
			repository().save(p1);
			repository().deleteAll(Arrays.asList(p1));

			tx.commit();

		} finally {
			if (tx != null) {
				tx.close();
			}
		}
	}
	

	@Test
	public void rollback_correctBehaviour() throws IOException {
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
			
			tx.rollback();

		} finally {
			if (tx != null) {
				tx.close();
			}
		}

		//current tx shoul be roll backed so there must be only 1 item
		assertThat(repository().count()).isOne();
		
		//and that item should be in previous state
		final Pojo loaded = repository().findOne(pojo.getId());
		assertThat(loaded.getIntValue()).isEqualTo(1);
		assertThat(loaded.getStringValue()).isEqualTo("1");
	}
	
}

class BuggyFileProvider extends ImmediateFlushStorageBackend<String> {

	private boolean buggy;
	
	private int counter;
	
	public BuggyFileProvider(PojoDB pojoDB, String collectionName, IFileFormatStrategy fileFormatStrategy) {
		super(pojoDB, collectionName, fileFormatStrategy, s -> s);
	}

	public void activateHellMachine() {
		buggy = true;
	}
	
	public void pleaseStahp() {
		buggy = false;
	}

	@Override
	public void save(String id, String data, TxContext context) throws IOException {
		if (buggy && ++counter % 3 == 0) {
			throw new IOException("Buggy file provider save");
		}
		
		super.save(id, data, context);
	}

	@Override
	public void delete(String id, TxContext context) throws IOException {
		if (buggy && ++counter % 3 == 0) {
			throw new IOException("Buggy file provider delete");
		}
		
		super.delete(id, context);
	}
}
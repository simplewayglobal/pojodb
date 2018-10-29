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

package global.simpleway.pojodb.bo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import global.simpleway.pojodb.PojoDB;
import global.simpleway.pojodb.repository.DomainRepository;
import global.simpleway.pojodb.repository.Tx;

public class Usage {

	public static void main(String[] args) {
		final PojoDB storage = PojoDB.builder()
				.withPath("/Users/miroslavhruz/upis/IdeaProjects/bos/flat-file-storage")
				.build();

		final DomainRepository<User> userRepository = storage.newDomainRepository("generic.users", User.class);

		final User user = new User();
		user.setUsername("john");
		user.setPassword("doe");

		userRepository.save(user);

		user.setId(null);
		user.setUsername("mira");
		user.setPassword("hruz");

		userRepository.save(user);

		user.setId(null);
		user.setUsername("david");
		user.setPassword("tetour");

		userRepository.save(user);

		final List<User> all = userRepository.findAll();
		all.forEach(u -> System.out.println("Got: " + u.getUsername()));

		final DomainRepository<QueueItem> queueRepository = storage.newDomainRepository("vox.queue", QueueItem.class);

		final QueueItem item = new QueueItem();

		item.setInputs(Arrays.asList("1", "2", "3"));
		item.setState("testing");
		item.setZones(Arrays.asList("a", "b"));

		queueRepository.save(item);

		Tx tx = null;
		try {
			tx = storage.newTx();

			item.setState("MIRA");
			item.setId(UUID.randomUUID().toString());
			queueRepository.save(item);

			item.setState("MIRA2");
			item.setId(UUID.randomUUID().toString());
			queueRepository.save(item);

			User user2 = new User();
			user2.setId(UUID.randomUUID().toString());
			user2.setUsername("daksmdkmadkms");
			userRepository.save(user2);

			userRepository.delete(user);

			try {
				tx.commit();
			} catch (IOException e1) {
				e1.printStackTrace();
				tx.rollback();

				throw e1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (tx != null) {
				tx.close();
			}
		}

	}
}

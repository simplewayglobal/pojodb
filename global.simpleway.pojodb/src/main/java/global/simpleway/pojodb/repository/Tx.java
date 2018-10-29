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

package global.simpleway.pojodb.repository;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;

import global.simpleway.pojodb.PojoDB;

public class Tx extends AbstractTx {

	private final PojoDB pojoDB;

	/*package*/ Tx(PojoDB pojoDB) {
		super(pojoDB.getRepositories().isEmpty() ? null : pojoDB.getRepositories().iterator().next(), new CompositeWriteCommand<>());

		this.pojoDB = pojoDB;
	}

	public Tx withCommand(Repository<?, ?> repository, IWriteCommand writeCommand) {
		getCompositeCommand().add(repository.getTypeClass(), writeCommand);
		return this;
	}

	public boolean isAnyCreate(Object id) {
		return isAnyCreate(getCompositeCommand().getClassesAndCommands(), id);
	}

	private static boolean isAnyCreate(List<Pair<Class<?>, IWriteCommand>> classesAndCommands, Object id) {
		Preconditions.checkArgument(classesAndCommands != null);

		return classesAndCommands.stream()
				.map(Pair::getRight)
				.filter(c -> {
					if (c instanceof SaveWriteCommand<?, ?>) {
						return true;
					} else if (c instanceof SaveListWriteCommand<?> == false) {
						return false;
					} else {
						SaveListWriteCommand<?> listWriteCommand = (SaveListWriteCommand<?>) c;

						return isAnyCreate(listWriteCommand.getClassesAndCommands(), id);
					}
				})
				.filter(c -> {
					SaveWriteCommand<?, ?> saveWriteCommand = (SaveWriteCommand<?, ?>) c;

					return Objects.equals(saveWriteCommand.getId(), id);
				})
				.findAny()
				.orElse(null) != null;
	}

	private CompositeWriteCommand<?> getCompositeCommand() {
		return (CompositeWriteCommand<?>) writeCommand;
	}

	@Override
	public void close() {
		try {
			super.close();
		} finally {
			pojoDB.getTxManager().endTx();
		}

	}
}

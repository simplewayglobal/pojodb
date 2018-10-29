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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;

import global.simpleway.pojodb.utils.ExceptionUtils;
import global.simpleway.pojodb.utils.LogUtil;

public class CompositeWriteCommand<T> extends AWriteCommand {

	private static final long serialVersionUID = 6112862806328383764L;

	private List<Pair<Class<?>, IWriteCommand>> classesAndCommands = new ArrayList<>();

	public void add(Class<?> typeClass, IWriteCommand command) {
		Preconditions.checkArgument(typeClass != null, "Could not have null type class");
		Preconditions.checkArgument(command != null, "Could not have null command");

		classesAndCommands.add(Pair.of(typeClass, command));
	}

	public List<Pair<Class<?>, IWriteCommand>> getClassesAndCommands() {
		return new ArrayList<>(classesAndCommands);
	}

	@Override
	public void backup(Repository<?, ?> anyRepository, TxContext context) throws IOException {
		delegateWork(() -> LogUtil.build("Could not backup files for write command: {}", this), anyRepository, (repository, writeCommand) -> {
			try {
				//delegate work to the underlying repository for correct storage and file format strategy
				writeCommand.backup(repository, context);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		});
	}

	@Override
	public void commit(Repository<?, ?> anyRepository, TxContext context) throws IOException {
		delegateWork(() -> LogUtil.build("Could not persist files for write command: {}", this), anyRepository, (repository, writeCommand) -> {
			try {
				//delegate work to the underlying repository for correct storage and file format strategy
				writeCommand.commit(repository, context);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		});
	}

	@Override
	public void rollback(Repository<?, ?> anyRepository, TxContext context) throws IOException {
		delegateWork(() -> LogUtil.build("Could not rollback for write command: {}", this), anyRepository, (repository, writeCommand) -> {
			try {
				//delegate work to the underlying repository for correct storage and file format strategy
				writeCommand.rollback(repository, context);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		});
	}

	@Override
	public void clearBackup(Repository<?, ?> anyRepository, TxContext context) throws IOException {
		delegateWork(() -> LogUtil.build("Could not clear backup files for write command: {}", this), anyRepository, (repository, writeCommand) -> {
			try {
				//delegate work to the underlying repository for correct storage and file format strategy
				writeCommand.clearBackup(repository, context);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		});
	}

	private void delegateWork(Supplier<String> logSupplier, Repository<?, ?> anyRepository, BiConsumer<Repository<?, ?>, IWriteCommand> commandConsumer) throws IOException {
		try {
			ExceptionUtils.iterateAllAndThrowIfAnyException(logSupplier, classesAndCommands, pair -> {
				final Repository<?, ?> repo = anyRepository.getPojoDB().getRepository(pair.getLeft());
				final IWriteCommand writeCommand = pair.getRight();

				commandConsumer.accept(repo, writeCommand);
			});
		} catch (RuntimeException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			}
			throw new IOException("Unrecognized exception", e);
		}
	}
}

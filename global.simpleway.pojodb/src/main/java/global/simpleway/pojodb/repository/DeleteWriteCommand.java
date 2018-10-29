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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteWriteCommand<T,ID> extends ASingleIdWriteCommand<T,ID> {

	private static final long serialVersionUID = -8397624838089789982L;
	
	private final Class<T> itemClass;
	
	@JsonCreator
	public DeleteWriteCommand(@JsonProperty("id") ID id, @JsonProperty("itemClass") Class<T> itemClass) {
		super(id);
		
		this.itemClass = itemClass;
	}

	public Class<T> getItemClass() {
		return itemClass;
	}

	@Override
	public void backup(Repository<?, ?> repository, TxContext context) throws IOException {
		repository.backup_single(this, context);
	}

	@Override
	public void commit(Repository<?, ?> repository, TxContext context) throws IOException {
		repository.commit_delete(this, context);
	}

	@Override
	public void rollback(Repository<?, ?> repository, TxContext context) throws IOException {
		repository.rollback_single(this, context);
	}

	@Override
	public void clearBackup(Repository<?, ?> repository, TxContext context) throws IOException {
		repository.clearBackup_single(this, context);
	}

	public static <T, ID> List<IWriteCommand> create(List<T> items, Function<T, ID> idSupplier) {
		if (items == null) return Collections.emptyList();

		return items.stream()
				.map(i -> new DeleteWriteCommand<>(idSupplier.apply(i), i.getClass()))
				.collect(Collectors.toList());
	}

}

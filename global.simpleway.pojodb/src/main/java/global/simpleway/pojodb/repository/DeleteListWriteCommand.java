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
import java.util.function.Function;

public class DeleteListWriteCommand<T> extends CompositeWriteCommand<T> {

	private static final long serialVersionUID = -9187223085519463639L;

	public static <T,ID> DeleteListWriteCommand<T> create(List<T> list, Class<T> typeClass, Function<T, ID> idSupplier) {
		final DeleteListWriteCommand<T> writeCommand = new DeleteListWriteCommand<>();

		DeleteWriteCommand.create(list, idSupplier)
				.forEach(command -> {
					writeCommand.add(typeClass, command);
				});
		
		return writeCommand;
	}
	
}

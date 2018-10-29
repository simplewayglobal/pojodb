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

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class TxContext {

	private String opId;
	
	private Class<?> typeClass;

	public String getOpId() {
		return opId;
	}

	public void setOpId(String opId) {
		this.opId = opId;
	}

	public Class<?> getTypeClass() {
		return typeClass;
	}

	public void setTypeClass(Class<?> typeClass) {
		this.typeClass = typeClass;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TxContext)) return false;
		final TxContext txContext = (TxContext) o;
		return Objects.equals(getOpId(), txContext.getOpId()) && // NOSONAR 
				Objects.equals(getTypeClass(), txContext.getTypeClass());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getOpId(), getTypeClass());
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}

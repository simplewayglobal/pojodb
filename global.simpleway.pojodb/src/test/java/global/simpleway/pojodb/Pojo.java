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

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Pojo extends AJacksonDomainObject implements Cloneable {

	private String id;

	private Integer intValue;
	private String stringValue;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public Integer getIntValue() {
		return intValue;
	}

	public void setIntValue(Integer intValue) {
		this.intValue = intValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Pojo)) return false;
		final Pojo pojo = (Pojo) o;
		return Objects.equals(getId(), pojo.getId()) && // NOSONAR 
				Objects.equals(getIntValue(), pojo.getIntValue()) &&
				Objects.equals(getStringValue(), pojo.getStringValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getIntValue(), getStringValue());
	}

	@Override
	protected Pojo clone() {
		final Pojo pojo = new Pojo();
		pojo.setId(getId());
		pojo.setIntValue(getIntValue());
		pojo.setStringValue(getStringValue());
		return pojo;
	}
}

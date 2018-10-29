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

import java.io.IOException;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import global.simpleway.pojodb.format.JsonFileFormatStrategy;

public class JsonFileFormatStrategyTest {

	@Test(expected = IOException.class)
	public void noDefaultConstructor() throws IOException {
		final JsonFileFormatStrategy fileFormatStrategy = new JsonFileFormatStrategy();

		final NoDefaultConstructor pojo = new NoDefaultConstructor("1", "1");
		final String stringValue = fileFormatStrategy.toString(pojo);

		fileFormatStrategy.fromString(stringValue, NoDefaultConstructor.class);
	}

	@Test
	public void defaultConstructor() throws IOException {
		final JsonFileFormatStrategy fileFormatStrategy = new JsonFileFormatStrategy();

		final DefaultConstructor pojo = new DefaultConstructor("1", "1");
		final String stringValue = fileFormatStrategy.toString(pojo);

		final Object loaded = fileFormatStrategy.fromString(stringValue, DefaultConstructor.class);
		Assertions.assertThat(loaded).isEqualTo(pojo);
	}

}

class NoDefaultConstructor extends AJacksonDomainObject {

	private String id;

	private String value;

	public NoDefaultConstructor(String id, String value) {
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
		if (!(o instanceof NoDefaultConstructor)) return false;
		final NoDefaultConstructor that = (NoDefaultConstructor) o;
		return Objects.equals(getId(), that.getId()) && // NOSONAR 
				Objects.equals(getValue(), that.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getValue());
	}
}

class DefaultConstructor extends AJacksonDomainObject {

	private String id;

	private String value;

	@JsonCreator
	public DefaultConstructor(@JsonProperty("id") String id, @JsonProperty("value") String value) {
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
		if (!(o instanceof DefaultConstructor)) return false;
		final DefaultConstructor that = (DefaultConstructor) o;
		return Objects.equals(getId(), that.getId()) && // NOSONAR 
				Objects.equals(getValue(), that.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getValue());
	}
}

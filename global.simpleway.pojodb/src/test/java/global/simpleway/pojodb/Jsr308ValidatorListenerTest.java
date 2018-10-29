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

import java.time.ZonedDateTime;
import java.util.Objects;

import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.junit.Test;

import global.simpleway.pojodb.listener.Jsr308ValidatorListener;

public class Jsr308ValidatorListenerTest {

	private Jsr308ValidatorListener<AlwaysCheckValidationBeforeSave> listener = new Jsr308ValidatorListener<>();

	@Test
	public void valid() {
		final AlwaysCheckValidationBeforeSave pojo = new AlwaysCheckValidationBeforeSave();

		pojo.setId("1");
		pojo.setEmail("miroslav.hruz@simpleway.space");
		pojo.setDate(ZonedDateTime.now().plusHours(1));
		pojo.setName("Miroslav Hruz");

		listener.validate(pojo);
	}

	@Test(expected = ConstraintViolationException.class)
	public void notValidEmail() {
		final AlwaysCheckValidationBeforeSave pojo = new AlwaysCheckValidationBeforeSave();

		pojo.setId("1");
		pojo.setEmail("www.simpleway.global");
		pojo.setDate(ZonedDateTime.now().plusHours(1));
		pojo.setName("Miroslav Hruz");

		listener.validate(pojo);
	}

	@Test(expected = ConstraintViolationException.class)
	public void notValidId() {
		final AlwaysCheckValidationBeforeSave pojo = new AlwaysCheckValidationBeforeSave();

		pojo.setId(null);
		pojo.setEmail("miroslav.hruz@simpleway.space");
		pojo.setDate(ZonedDateTime.now().plusHours(1));
		pojo.setName("Miroslav Hruz");

		listener.validate(pojo);
	}

	@Test(expected = ConstraintViolationException.class)
	public void notValidDate() {
		final AlwaysCheckValidationBeforeSave pojo = new AlwaysCheckValidationBeforeSave();

		pojo.setId("1");
		pojo.setEmail("miroslav.hruz@simpleway.space");
		pojo.setDate(ZonedDateTime.now().minusHours(1));
		pojo.setName("Miroslav Hruz");

		listener.validate(pojo);
	}

	@Test(expected = ConstraintViolationException.class)
	public void notValidName() {
		final AlwaysCheckValidationBeforeSave pojo = new AlwaysCheckValidationBeforeSave();

		pojo.setId("1");
		pojo.setEmail("miroslav.hruz@simpleway.space");
		pojo.setDate(ZonedDateTime.now().plusHours(1));
		pojo.setName("     ");

		listener.validate(pojo);
	}

}

class AlwaysCheckValidationBeforeSave extends AJacksonDomainObject {

	@NotNull
	private String id;

	@Email
	private String email;

	@NotBlank
	private String name;

	@Future
	private ZonedDateTime date;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ZonedDateTime getDate() {
		return date;
	}

	public void setDate(ZonedDateTime date) {
		this.date = date;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AlwaysCheckValidationBeforeSave)) return false;
		final AlwaysCheckValidationBeforeSave that = (AlwaysCheckValidationBeforeSave) o;
		return Objects.equals(getId(), that.getId()) && // NOSONAR 
				Objects.equals(getEmail(), that.getEmail()) &&
				Objects.equals(getName(), that.getName()) &&
				Objects.equals(getDate(), that.getDate());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getEmail(), getName(), getDate());
	}
}
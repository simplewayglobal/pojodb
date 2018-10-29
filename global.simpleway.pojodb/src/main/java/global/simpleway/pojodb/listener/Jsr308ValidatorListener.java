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

package global.simpleway.pojodb.listener;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import global.simpleway.pojodb.utils.LogUtil;

public class Jsr308ValidatorListener<T> extends APojoDBListenerAdapter<T> {

	private final Validator validator;

	public Jsr308ValidatorListener() {
		final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
		this.validator = validatorFactory.getValidator();
	}
	
	
	public void validate(T pojo) {
		final Set<ConstraintViolation<T>> violations = validator.validate(pojo);

		if (violations.isEmpty() == false) {
			StringBuilder constraintValidationError = new StringBuilder();
			Set<ConstraintViolation<?>> set = new HashSet<>();
			for (ConstraintViolation<T> validationError : violations) {
				set.add(validationError);
				constraintValidationError.append(LogUtil.build("\nProperty: [{}] on class: [{}]: {}", validationError.getPropertyPath(), validationError.getRootBeanClass(),
						validationError.getMessage()));
			}
			throw new ConstraintViolationException(constraintValidationError.toString(), set);
		}
	}
	
	@Override
	public void onBeforeSave(T pojo) {
		validate(pojo);
	}
}

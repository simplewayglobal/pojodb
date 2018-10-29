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

package global.simpleway.pojodb.format;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/**
 * Uses jackson configured elsewhere e.g. in Spring
 * 
 */
public class ExternalJacksonFileFormatStrategy extends JsonFileFormatStrategy {
	
	private final ObjectMapper externalJackson;

	public ExternalJacksonFileFormatStrategy(ObjectMapper externalJackson) {
		Preconditions.checkArgument(externalJackson != null, "Must provide valid instance of jackson");
		
		this.externalJackson = externalJackson;
	}

	@Override
	public String toString(Object type) throws IOException {
		return externalJackson.writeValueAsString(type);
	}

	@Override
	public <T> T fromString(String data, Class<T> clazz) throws IOException {
		return externalJackson.readValue(data, clazz);
	}
}

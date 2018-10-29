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

/**
 * JSON file format for the flat file storage
 * 
 */
public class JsonFileFormatStrategy implements IFileFormatStrategy {
	
	// thread safe instance of the jackson configured on default values
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public String getFileExtension() {
		return ".json";
	}

	@Override
	public String toString(Object type) throws IOException {
		return OBJECT_MAPPER.writeValueAsString(type);
	}

	@Override
	public <T> T fromString(String data, Class<T> clazz) throws IOException {
		return OBJECT_MAPPER.readValue(data, clazz);
	}
}

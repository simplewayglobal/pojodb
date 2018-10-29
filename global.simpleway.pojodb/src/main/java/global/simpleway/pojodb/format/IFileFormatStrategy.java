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

/**
 * BO to String (de)serializer 
 * 
 */
public interface IFileFormatStrategy {

	public String getFileExtension();
	
	
	/**
	 * Serializes BO to String to the specific file format
	 * 
	 * @param type to be serialized
	 * @return
	 * @throws IOException
	 */
	public String toString(Object type) throws IOException;

	/**
	 * Deserialize raw string data to the BO
	 * 
	 * @param data raw data to be deserialized
	 * @param clazz of the result to be deserialized
	 * @return
	 * @throws IOException
	 */
	public <T> T fromString(String data, Class<T> clazz) throws IOException;
	
}

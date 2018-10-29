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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

public class NioFileUtils {

	public static String read(Path path) throws IOException {
		return Files.readAllLines(path).stream()
				.collect(Collectors.joining());
	}

	public static boolean exists(Path path) {
		return Files.isReadable(path) && Files.isDirectory(path) == false;
	}

	public static void save(Path path, String data) throws IOException {
		FileUtils.writeStringToFile(path.toFile(), data, StandardCharsets.UTF_8);
	}

	public static void rename(Path oldPath, Path newPath) throws IOException {
		Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
	}

	public static void copy(Path oldPath, Path newPath) throws IOException {
		Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
	}

	public static void delete(Path path) throws IOException {
		Files.delete(path);
	}
}

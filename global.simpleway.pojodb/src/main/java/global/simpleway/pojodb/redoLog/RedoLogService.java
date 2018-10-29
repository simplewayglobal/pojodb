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

package global.simpleway.pojodb.redoLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;

import global.simpleway.pojodb.PojoDB;
import global.simpleway.pojodb.format.IFileFormatStrategy;
import global.simpleway.pojodb.id.IIdGenerator;
import global.simpleway.pojodb.repository.IWriteCommand;
import global.simpleway.pojodb.repository.NioFileUtils;
import global.simpleway.pojodb.utils.ExceptionUtils;
import global.simpleway.pojodb.utils.LogUtil;

public class RedoLogService {

	private final PojoDB pojoDB;

	private final IFileFormatStrategy fileFormat;

	private final IIdGenerator<Object,String> idGenerator;
	
	public RedoLogService(PojoDB pojoDB, IFileFormatStrategy fileFormat, IIdGenerator<Object,String> idGenerator) {
		this.pojoDB = pojoDB;
		this.fileFormat = fileFormat;
		this.idGenerator = idGenerator;
	}
	
	public Map<String, IWriteCommand> getAllIncompleteTxs() {
		final List<File> allIncompleteTx = Arrays.asList(getRedoLogPath().toFile().listFiles());
		
		//must ensure order to replay TXes
		final Map<String, IWriteCommand> mapToProcess = new TreeMap<>();

		ExceptionUtils.iterateAllAndThrowIfAnyException(() -> LogUtil.build("Could not get all incomplete tx"), allIncompleteTx, f -> {
			try {
				final String content = NioFileUtils.read(f.toPath());

				final IWriteCommand writeCommand = fileFormat.fromString(content, IWriteCommand.class);
				
				mapToProcess.put(FilenameUtils.getBaseName(f.getName()), writeCommand);

			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		});
		
		return mapToProcess;
	}

	public String txBegin(IWriteCommand command) throws IOException {
		final String opId = idGenerator.getNextId();
		final String jsonCommand = fileFormat.toString(command);

		final Path txPath = getTxLogPath(opId);

		synchronized (pojoDB.intern(txPath)) {
			NioFileUtils.save(txPath, jsonCommand);
		}

		return opId;
	}

	public void txEnd(String opId) throws IOException {
		final Path txPath = getTxLogPath(opId);

		synchronized (pojoDB.intern(txPath)) {
			NioFileUtils.delete(txPath);
		}
	}

	private Path getTxLogPath(String opId) {
		return getRedoLogPath().resolve(opId + fileFormat.getFileExtension());
	}

	private Path getRedoLogPath() {
		return pojoDB.getStoragePath().resolve("_redo.log");
	}
	
}

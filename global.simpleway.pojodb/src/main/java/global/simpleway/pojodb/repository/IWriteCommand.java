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
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import global.simpleway.pojodb.redoLog.RedoLogService;

/**
 * Serializable DTO to represent a write command
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(alphabetic = true)
public interface IWriteCommand extends Serializable { 
	
	public String begin(RedoLogService redoLogService) throws IOException;
	
	public void backup(Repository<?, ?> repository, TxContext context) throws IOException;
	
	public void commit(Repository<?, ?> repository, TxContext context) throws IOException;
	
	public void rollback(Repository<?, ?> repository, TxContext context) throws IOException;
	
	public void clearBackup(Repository<?, ?> repository, TxContext context) throws IOException;
	
	public void end(RedoLogService redoLogService, String opId) throws IOException;
	
}

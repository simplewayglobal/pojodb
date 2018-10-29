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

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import global.simpleway.pojodb.utils.LogUtil;

public abstract class AbstractTx implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(AbstractTx.class);

	protected TxContext context = new TxContext();

	protected final IWriteCommand writeCommand;
	protected final Repository<?, ?> repository;
	protected volatile boolean allFilesBackedUp = false;

	public AbstractTx(Repository<?, ?> repository, IWriteCommand writeCommand) {
		this.writeCommand = writeCommand;
		this.repository = repository;
	}

	public void commitOrRollback() throws IOException {
		try {
			commit();
		} catch (IOException e) {
			logger.warn("Persist did not work for context: {} and write command: {}", context, writeCommand, e);

			//main cleanup feature, persist did not worked so rollback if possible and end transaction and clear things
			doRollback(e);

			//persist did not work but successfully rollbacked
			throw e;
		}

		//cleanup will be done in closable
	}

	public void commit() throws IOException {
		doBegin();
		doCommit();
	}

	/**
	 * Used only for replay incomplete transaction, reuse of original item in redo log
	 *
	 * @param originalOpId
	 * @throws IOException
	 */
	public void skipBegin(String originalOpId) throws IOException {
		context.setOpId(originalOpId);
		allFilesBackedUp = true;
	}

	/**
	 * Prepare transaction
	 *
	 * @throws IOException
	 */
	protected void doBegin() throws IOException {
		if (allFilesBackedUp) return;

		try {
			//1. mark begin of new operation
			final String opId = writeCommand.begin(repository.getRedoLogService());
			context.setOpId(opId);

			//2. copy old values
			writeCommand.backup(this.repository, context);

			//mark here that we have all files backed up correctly, even commit operation was called and
			allFilesBackedUp = true;
		} catch (IOException e) {
			//could not start new transaction in redo log or could not backup previous value
			//do not start transaction at all and fail fast
			throw new IOException(LogUtil.build("Could not start transaction: {}", writeCommand), e);
		}
	}

	protected void doCommit() throws IOException {
		writeCommand.commit(repository, context);
	}

	public void rollback() throws IOException {
		doRollback(null);
	}

	protected void doRollback(IOException e) throws IOException {
		//if nobody calls commit or all files was not backed up correctly rollback does nothing
		if (allFilesBackedUp == false) return;

		//3a. rollback if possible
		try {
			writeCommand.rollback(repository, context);
		} catch (IOException e1) {
			//persist did not worked and neither rollback did not work, so this is little bit silly

			//do not end transaction
			//do not clear backup, because in backup we have last good value
			//log error, add suppress to the original exception and leave state as is.

			if (e != null) {
				logger.error("Rollback failed for context: {} and write command. Could not repair it automatically.", context, writeCommand, e1);
				e.addSuppressed(e1);

				throw e;
			}

			throw e1;
		}

		//3b. clear transaction, clear backup
		doCleanup(e);
	}

	public void cleanup() throws IOException {
		doCleanup(null);
	}

	protected void doCleanup(IOException e) throws IOException {
		try {
			//we don't have opId, commit did not happen
			if (allFilesBackedUp == false) return;

			writeCommand.end(repository.getRedoLogService(), context.getOpId());

			//clear backup
			doClearBackupQuietly();
		} catch (IOException e1) {
			//persist did not work, but rollbacked and could not end transaction or clear backup file

			logger.warn("Rollbacked but could not end transaction or clear backup for context: {} and writeCommand: {}", context, writeCommand, e1);

			if (e != null) {
				e.addSuppressed(e1);

				throw e;
			}

			throw e1;
		}

	}

	protected void doClearBackupQuietly() {
		//rollback if possible
		try {
			writeCommand.clearBackup(repository, context);
		} catch (IOException e) {
			logger.warn("Could not clean backup: {}", writeCommand, e);
		}
	}

	@Override
	public void close() {
		try {
			cleanup();
		} catch (IOException e) {
			logger.warn("Could not close", e);
		}
	}

}

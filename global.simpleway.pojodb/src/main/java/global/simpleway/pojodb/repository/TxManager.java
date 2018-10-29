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

import global.simpleway.pojodb.PojoDB;

public class TxManager {
	
	// there is any outgoing transaction in current execution thread
	private final ThreadLocal<Tx> txHolder = new ThreadLocal<>();
	
	public boolean isInAutocommit() {
		return getCurrentTx() == null;
	}
	
	public Tx getCurrentTx() {
		return txHolder.get();
	}
	
	public Tx newTx(PojoDB pojoDB) {
		final Tx tx = new Tx(pojoDB);
		
		txHolder.set(tx);
		
		return tx;
	}
	
	public void endTx() {
		txHolder.remove();
	}
	
	
}

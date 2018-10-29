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

package global.simpleway.pojodb.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Way how to re-use messages in logback and java exceptions in same way
 * 
 * @author miroslavhruz
 * @author tklempa
 * 
 */
public final class LogUtil {

	/**
	 * Counter to easy identify particular exception in the log which was not directly shown to the user. e.g.: see server log for info [ref=2]
	 */
	private static final AtomicLong REFERENCE_COUNTER = new AtomicLong(0);

	private LogUtil() {
		//no code
	}

	/**
	 * Message will replace {} by args in order
	 *
	 * @param message
	 * @param args
	 * @return
	 */
	public static Supplier<String> supply(String message, Object... args) {
		return () -> LogUtil.build(message, args);
	}

	/**
	 * Message will replace {} by args in order
	 * 
	 * @param message
	 * @param args
	 * @return
	 */
	public static String build(String message, Object... args) {
		List<String> arguments = new ArrayList<>();
		for (Object arg : args) {
			arguments.add(String.valueOf(arg));
		}
		return build(message, arguments.toArray(new String[arguments.size()]));

	}

	/**
	 * Message will replace {} by args in order
	 * 
	 * @param message
	 * @param args
	 * @return
	 */
	public static String build(String message, String... args) {
		if (message == null) {
			return null;
		}

		if (message.indexOf("{}") == -1) {
			return message;
		}

		for (int i = 0; i < args.length; i++) {
			try {
				message = message.replaceFirst("\\{\\}", args[i] == null ? "null" : args[i]);
			} catch (RuntimeException e) { //NOSONAR
				// could not replace character
				message += args[i];
			}
		}

		return message;
	}

	/**
	 * Creates log reference marker which can be included with the log message to easy identify it. e.g.: when showing
	 * "see server log for more info [logRef=1]" you can easily search for logRef=1 to find referencing error in the log
	 * 
	 * @return
	 */
	public static String createReferenceMarker() {
		return "[logRef=" + REFERENCE_COUNTER.incrementAndGet() + "]";
	}

}

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

/**
 * For-each separation of exceptions utils
 * 
 * @author miroslavhruz
 * @author davidkadlecek
 *
 */
public class ExceptionUtils {
	public static final int DEFAULT_MAX_SUPPRESSED_EXCEPTIONS_COUNT = 20;

	/**
	 * Convenient method calling {@link #runAllAndThrowIfAnyException(Supplier, int, Runnable...)} with {@link #DEFAULT_MAX_SUPPRESSED_EXCEPTIONS_COUNT} as maximum of suppressed
	 * exceptions.
	 * 
	 * @param exceptionMessageSupplier
	 * @param actions
	 */
	public static void runAllAndThrowIfAnyException(Supplier<String> exceptionMessageSupplier, Runnable... actions) {
		runAllAndThrowIfAnyException(exceptionMessageSupplier, DEFAULT_MAX_SUPPRESSED_EXCEPTIONS_COUNT, actions);
	}

	/**
	 * Perform sequence of actions separately and throw only one exception if any exceptions occurred
	 * 
	 * @param exceptionMessageSupplier
	 *            log callback
	 * @param maxSuppressedExceptionsCount
	 *            maximum count of exception added to the RuntimeException throw in the end. The following exceptions are throw away
	 * @param actions
	 */
	public static void runAllAndThrowIfAnyException(Supplier<String> exceptionMessageSupplier, int maxSuppressedExceptionsCount, Runnable... actions) {
		Preconditions.checkArgument(exceptionMessageSupplier != null);
		Preconditions.checkArgument(actions != null && actions.length > 0);

		RuntimeException original = null;
		int suppressedExpCounter = 0;
		for (Runnable runnable : actions) {
			try {
				runnable.run();
			} catch (RuntimeException e) {
				// skip exception handling if there is already too many suppressed exceptions
				if (suppressedExpCounter > maxSuppressedExceptionsCount) {
					continue;
				}

				if (original == null) {
					original = new RuntimeException(exceptionMessageSupplier.get());
				}
				original.addSuppressed(e);
				suppressedExpCounter++;
			}
		}
		if (original != null) {
			throw original;
		}
	}

	/**
	 * Perform sequence of actions separately and throw only one exception if any exceptions occurred
	 * 
	 * @param exceptionMessageSupplier
	 *            log callback
	 * @param instance
	 * @param consumers
	 *            actions
	 */
	@SafeVarargs
	public static <T> void runAllAndThrowIfAnyException(Supplier<String> exceptionMessageSupplier, T instance, Consumer<T>... consumers) {
		Preconditions.checkArgument(exceptionMessageSupplier != null);
		Preconditions.checkArgument(consumers != null && consumers.length > 0);

		List<Consumer<T>> consumerList = Arrays.asList(consumers);
		ArrayList<Runnable> runnableList = new ArrayList<>();

		consumerList.forEach(consumer -> runnableList.add(() -> {
			consumer.accept(instance);
		}));

		runAllAndThrowIfAnyException(exceptionMessageSupplier, runnableList.toArray(new Runnable[runnableList.size()]));
	}

	/**
	 * Perform one action on sequence of entities separately and throw only one exception if any exceptions occurred <b>Provide type for consumer! See
	 * cz.sw.upis.util.ExceptionUtilsInvokeAllInstancesTest#normalExecution</b>
	 *
	 * @param exceptionMessageSupplier
	 *            log callback
	 * @param entities
	 * @param consumer
	 *            action
	 */
	public static <T> void iterateAllAndThrowIfAnyException(Supplier<String> exceptionMessageSupplier, Iterable<? extends T> entities, Consumer<T> consumer) {
		iterateAllWithResultAndThrowIfAnyException(exceptionMessageSupplier, entities, item -> {
			consumer.accept(item);
			return null;
		});
	}

	/**
	 * Perform one action on sequence of entities separately and throw only one exception if any exceptions occurred
	 *
	 * @param exceptionMessageSupplier
	 *            log callback
	 * @param entities
	 * @param function
	 *            action
	 */
	public static <T,R> List<R> iterateAllWithResultAndThrowIfAnyException(Supplier<String> exceptionMessageSupplier, Iterable<? extends T> entities, Function<T, R> function) {
		Preconditions.checkArgument(exceptionMessageSupplier != null);
		Preconditions.checkArgument(function != null);
		// should not throw if performing on empty collection, same behavior as #runAllAndThrowIfAnyException for null instance
		if (entities == null || entities.iterator().hasNext() == false)
			return Collections.emptyList();

		ArrayList<Runnable> runnableList = new ArrayList<>();

		ArrayList<R> results = new ArrayList<>();
		entities.forEach(entity -> runnableList.add(() -> {
			results.add(function.apply(entity));
			return;
		}));
		runAllAndThrowIfAnyException(exceptionMessageSupplier, runnableList.toArray(new Runnable[runnableList.size()]));
		return results;
	}

	/**
	 * Perform sequence of entities separately and sequence of actions separately and throw only one exception if any exceptions occurred <b>Provide type for consumer! See
	 * cz.sw.upis.util.ExceptionUtilsInvokeAllInstancesTest#normalExecution</b>
	 *
	 * @param exceptionMessageSupplier
	 *            log callback
	 * @param entities
	 * @param consumers
	 *            actions
	 */
	public static <T> void iterateAllAndThrowIfAnyExceptionMultipleConsumers(Supplier<String> exceptionMessageSupplier, Iterable<? extends T> entities, List<Consumer<T>> consumers) {
		Preconditions.checkArgument(exceptionMessageSupplier != null);
		Preconditions.checkArgument(consumers != null && !consumers.isEmpty());
		// should not throw if performing on empty collection, same behavior as #runAllAndThrowIfAnyException for null instance
		if (entities == null || entities.iterator().hasNext() == false)	return;

		ArrayList<Runnable> runnableList = new ArrayList<>();
		entities.forEach(entity -> {
			consumers.forEach(consumer -> runnableList.add(() -> {
				consumer.accept(entity);
			}));
		});
		runAllAndThrowIfAnyException(exceptionMessageSupplier, runnableList.toArray(new Runnable[runnableList.size()]));
	}
}

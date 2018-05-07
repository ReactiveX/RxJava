/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.util;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.io.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReactiveInputStreamTest {
	@Test
	public void createFromNeverFlowable() throws Exception {
		InputStream is = ReactiveInputStream.toStrings(Flowable.<String>never());
		assertEquals(0, is.available());
	}

	@Test
	public void createFromNeverObservable() throws Exception {
		InputStream is = ReactiveInputStream.toStrings(Observable.<String>never());
		assertEquals(0, is.available());
	}

	@Test
	public void readFromStringFlowable() throws Exception {
		String str = "foo bar baz";
		InputStream is = ReactiveInputStream.toStrings(Flowable.fromArray(str.split(" ")));
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		assertEquals(str.replaceAll(" ", ""), br.readLine());
		assertEquals(-1, br.read());
		is.close();
	}

	@Test
	public void readFromStringObservable() throws Exception {
		String[] strings = { "foo", "bar", "baz" };
		InputStream is = ReactiveInputStream.toStrings(Observable.fromArray(strings));
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		assertEquals("foobarbaz", br.readLine());
		assertEquals(-1, br.read());
		is.close();
	}

	@Test
	public void readUsingToStringFlowable() throws Exception {
		String[] strings = { "foo", "bar", "baz" };
		InputStream is = ReactiveInputStream.toStrings(Flowable.fromArray(strings));
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		assertEquals("foobarbaz", br.readLine());
		assertEquals(-1, br.read());
		is.close();
	}

	@Test
	public void readFromStringWithDelimiterFlowable() throws Exception {
		String[] strings = { "foo", "bar", "baz" };
		InputStream is = ReactiveInputStream.toStrings(Flowable.fromArray(strings), "\n");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		for (String str : strings) {
			assertEquals(str, br.readLine());
		}
		assertEquals(-1, br.read());
		is.close();
	}

	@Test
	public void readFromStringWithDelimiterObservable() throws Exception {
		String[] strings = { "foo", "bar", "baz" };
		InputStream is = ReactiveInputStream.toStrings(Observable.fromArray(strings), "\n");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		for (String str : strings) {
			assertEquals(str, br.readLine());
		}
		assertEquals(-1, br.read());
		is.close();
	}

	@Test
	public void waits() throws Exception {
		// BufferedReader use can be problematic in that reading ahead for
		// buffering can block, hence terminating here with take().
		Scheduler scheduler = Schedulers.computation();
		Observable<String> obs = Observable.interval(1, 10, TimeUnit.MILLISECONDS, scheduler).
			map(new Function<Long, String>() {
				@Override
				public String apply(Long x) {
					return "done\n";
				}
			}).
			take(2);
		InputStream ois = ReactiveInputStream.toStrings(obs);
		BufferedReader br = new BufferedReader(new InputStreamReader(ois));
		assertEquals("done", br.readLine());
		ois.close();
	}

	@Test @SuppressWarnings("ResultOfMethodCallIgnored")
	public void passAlongExceptionAfterReading() throws Exception {
		class TestException extends Exception {}
		Exception e = new TestException();
		String str = "foo";
		Observable<String> obs = Observable.concat(
			Observable.just(str), Observable.<String>error(e));
		InputStream is = ReactiveInputStream.toStrings(obs);
		InputStreamReader reader = new InputStreamReader(is);

		for (byte i: str.getBytes()) {
			assertEquals(i, reader.read());
		}

		boolean thrown = false;
		try {
			reader.read();
		} catch (IOException ioe) {
			thrown = true;
			assertEquals(e, ioe.getCause());
		}
		assertTrue(thrown);
	}

	/**
	 * Document null behavior.  ReactiveInputStream can handle nulls, by
	 * ignoring them, but null observations are forbidden in RxJava.
	 */
	@Test
	public void documentNullBehavior() throws Exception {
		String[] strings = { "foo", null, "bar", "baz" };
		InputStream is = ReactiveInputStream.toStrings(Flowable.fromArray(strings));
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		try {
			br.readLine();
			fail("expecting NullPointerException");
		} catch (IOException e) {
			assertEquals(NullPointerException.class, e.getCause().getClass());
		}
	}
}

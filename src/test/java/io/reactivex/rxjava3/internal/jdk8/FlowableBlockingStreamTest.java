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

package io.reactivex.rxjava3.internal.jdk8;

import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.processors.UnicastProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FlowableBlockingStreamTest extends RxJavaTest {

    @Test
    public void empty() {
        try (Stream<Integer> stream = Flowable.<Integer>empty().blockingStream()) {
            assertEquals(0, stream.toArray().length);
        }
    }

    @Test
    public void just() {
        try (Stream<Integer> stream = Flowable.just(1).blockingStream()) {
            assertArrayEquals(new Integer[] { 1 }, stream.toArray(Integer[]::new));
        }
    }

    @Test
    public void range() {
        try (Stream<Integer> stream = Flowable.range(1, 5).blockingStream()) {
            assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, stream.toArray(Integer[]::new));
        }
    }

    @Test
    public void rangeBackpressured() {
        try (Stream<Integer> stream = Flowable.range(1, 5).blockingStream(1)) {
            assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, stream.toArray(Integer[]::new));
        }
    }

    @Test
    public void rangeAsyncBackpressured() {
        try (Stream<Integer> stream = Flowable.range(1, 1000).subscribeOn(Schedulers.computation()).blockingStream()) {
            List<Integer> list = stream.collect(Collectors.toList());

            assertEquals(1000, list.size());
            for (int i = 1; i <= 1000; i++) {
                assertEquals(i, list.get(i - 1).intValue());
            }
        }
    }

    @Test
    public void rangeAsyncBackpressured1() {
        try (Stream<Integer> stream = Flowable.range(1, 1000).subscribeOn(Schedulers.computation()).blockingStream(1)) {
            List<Integer> list = stream.collect(Collectors.toList());

            assertEquals(1000, list.size());
            for (int i = 1; i <= 1000; i++) {
                assertEquals(i, list.get(i - 1).intValue());
            }
        }
    }

    @Test
    public void error() {
        try (Stream<Integer> stream = Flowable.<Integer>error(new TestException()).blockingStream()) {
            stream.toArray(Integer[]::new);
            fail("Should have thrown!");
        } catch (TestException expected) {
            // expected
        }
    }

    @Test
    public void close() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        up.onNext(1);
        up.onNext(2);
        up.onNext(3);
        up.onNext(4);
        up.onNext(5);

        try (Stream<Integer> stream = up.blockingStream()) {
            assertArrayEquals(new Integer[] { 1, 2, 3 }, stream.limit(3).toArray(Integer[]::new));
        }

        assertFalse(up.hasSubscribers());
    }
}

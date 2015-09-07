/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import org.junit.*;
import org.reactivestreams.Subscriber;

import io.reactivex.*;

public class OperatorDoOnEachTest {

    Subscriber<String> subscribedObserver;
    Subscriber<String> sideEffectObserver;

    @Before
    public void before() {
        subscribedObserver = TestHelper.mockSubscriber();
        sideEffectObserver = TestHelper.mockSubscriber();
    }

    @Test
    public void testDoOnEach() {
        Observable<String> base = Observable.just("a", "b", "c");
        Observable<String> doOnEach = base.doOnEach(sideEffectObserver);

        doOnEach.subscribe(subscribedObserver);

        // ensure the leaf observer is still getting called
        verify(subscribedObserver, never()).onError(any(Throwable.class));
        verify(subscribedObserver, times(1)).onNext("a");
        verify(subscribedObserver, times(1)).onNext("b");
        verify(subscribedObserver, times(1)).onNext("c");
        verify(subscribedObserver, times(1)).onComplete();

        // ensure our injected observer is getting called
        verify(sideEffectObserver, never()).onError(any(Throwable.class));
        verify(sideEffectObserver, times(1)).onNext("a");
        verify(sideEffectObserver, times(1)).onNext("b");
        verify(sideEffectObserver, times(1)).onNext("c");
        verify(sideEffectObserver, times(1)).onComplete();
    }

    @Test
    public void testDoOnEachWithError() {
        Observable<String> base = Observable.just("one", "fail", "two", "three", "fail");
        Observable<String> errs = base.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                return s;
            }
        });

        Observable<String> doOnEach = errs.doOnEach(sideEffectObserver);

        doOnEach.subscribe(subscribedObserver);
        verify(subscribedObserver, times(1)).onNext("one");
        verify(subscribedObserver, never()).onNext("two");
        verify(subscribedObserver, never()).onNext("three");
        verify(subscribedObserver, never()).onComplete();
        verify(subscribedObserver, times(1)).onError(any(Throwable.class));

        verify(sideEffectObserver, times(1)).onNext("one");
        verify(sideEffectObserver, never()).onNext("two");
        verify(sideEffectObserver, never()).onNext("three");
        verify(sideEffectObserver, never()).onComplete();
        verify(sideEffectObserver, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void testDoOnEachWithErrorInCallback() {
        Observable<String> base = Observable.just("one", "two", "fail", "three");
        Observable<String> doOnEach = base.doOnNext(new Consumer<String>() {
            @Override
            public void accept(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
            }
        });

        doOnEach.subscribe(subscribedObserver);
        verify(subscribedObserver, times(1)).onNext("one");
        verify(subscribedObserver, times(1)).onNext("two");
        verify(subscribedObserver, never()).onNext("three");
        verify(subscribedObserver, never()).onComplete();
        verify(subscribedObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testIssue1451Case1() {
        // https://github.com/Netflix/RxJava/issues/1451
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i=0; i < expectedCount; i++) {
            Observable
                    .just(Boolean.TRUE, Boolean.FALSE)
                    .takeWhile(new Predicate<Boolean>() {
                        @Override
                        public boolean test(Boolean value) {
                            return value;
                        }
                    })
                    .toList()
                    .doOnNext(new Consumer<List<Boolean>>() {
                        @Override
                        public void accept(List<Boolean> booleans) {
                            count.incrementAndGet();
                        }
                    })
                    .subscribe();
        }
        assertEquals(expectedCount, count.get());
    }

    @Test
    public void testIssue1451Case2() {
        // https://github.com/Netflix/RxJava/issues/1451
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i=0; i < expectedCount; i++) {
            Observable
                    .just(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE)
                    .takeWhile(new Predicate<Boolean>() {
                        @Override
                        public boolean test(Boolean value) {
                            return value;
                        }
                    })
                    .toList()
                    .doOnNext(new Consumer<List<Boolean>>() {
                        @Override
                        public void accept(List<Boolean> booleans) {
                            count.incrementAndGet();
                        }
                    })
                    .subscribe();
        }
        assertEquals(expectedCount, count.get());
    }

    // FIXME crashing publisher can't propagate to a subscriber
//    @Test
//    public void testFatalError() {
//        try {
//            Observable.just(1, 2, 3)
//                    .flatMap(new Function<Integer, Observable<?>>() {
//                        @Override
//                        public Observable<?> apply(Integer integer) {
//                            return Observable.create(new Publisher<Object>() {
//                                @Override
//                                public void subscribe(Subscriber<Object> o) {
//                                    throw new NullPointerException("Test NPE");
//                                }
//                            });
//                        }
//                    })
//                    .doOnNext(new Consumer<Object>() {
//                        @Override
//                        public void accept(Object o) {
//                            System.out.println("Won't come here");
//                        }
//                    })
//                    .subscribe();
//            fail("should have thrown an exception");
//        } catch (OnErrorNotImplementedException e) {
//            assertTrue(e.getCause() instanceof NullPointerException);
//            assertEquals(e.getCause().getMessage(), "Test NPE");
//            System.out.println("Received exception: " + e);
//        }
//    }
}
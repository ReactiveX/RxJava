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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableSingleTest extends RxJavaTest {

    @Test
    public void singleFlowable() {
        Flowable<Integer> flowable = Flowable.just(1).singleElement().toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(1);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithTooManyElementsFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 2).singleElement().toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithEmptyFlowable() {
        Flowable<Integer> flowable = Flowable.<Integer> empty().singleElement().toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onComplete();
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleDoesNotRequestMoreThanItNeedsIf1Then2RequestedFlowable() {
        final List<Long> requests = new ArrayList<>();
        Flowable.just(1)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .singleElement()
                //
                .toFlowable()
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(2);
                    }
                });
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }

    @Test
    public void singleDoesNotRequestMoreThanItNeedsIf3RequestedFlowable() {
        final List<Long> requests = new ArrayList<>();
        Flowable.just(1)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .singleElement()
                //
                .toFlowable()
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                    }
                });
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }

    @Test
    public void singleRequestsExactlyWhatItNeedsIf1RequestedFlowable() {
        final List<Long> requests = new ArrayList<>();
        Flowable.just(1)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .singleElement()
                //
                .toFlowable()
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                    }
                });
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }

    @Test
    public void singleWithPredicateFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 2)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(2);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateAndTooManyElementsFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 2, 3, 4)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateAndEmptyFlowable() {
        Flowable<Integer> flowable = Flowable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toFlowable();
        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onComplete();
        inOrder.verify(subscriber, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultFlowable() {
        Flowable<Integer> flowable = Flowable.just(1).single(2).toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(1);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithTooManyElementsFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 2).single(3).toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithEmptyFlowable() {
        Flowable<Integer> flowable = Flowable.<Integer> empty()
                .single(1).toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(1);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 2)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4).toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(2);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateAndTooManyElementsFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 2, 3, 4)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6).toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateAndEmptyFlowable() {
        Flowable<Integer> flowable = Flowable.just(1)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2).toFlowable();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext(2);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithBackpressureFlowable() {
        Flowable<Integer> flowable = Flowable.just(1, 2).singleElement().toFlowable();

        Subscriber<Integer> subscriber = spy(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });
        flowable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void single() {
        Maybe<Integer> maybe = Flowable.just(1).singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        maybe.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithTooManyElements() {
        Maybe<Integer> maybe = Flowable.just(1, 2).singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        maybe.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithEmpty() {
        Maybe<Integer> maybe = Flowable.<Integer> empty().singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        maybe.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleDoesNotRequestMoreThanItNeedsToEmitItem() {
        final AtomicLong request = new AtomicLong();
        Flowable.just(1).doOnRequest(new LongConsumer() {
            @Override
            public void accept(long n) {
                request.addAndGet(n);
            }
        }).blockingSingle();
        // FIXME single now triggers fast-path
        assertEquals(Long.MAX_VALUE, request.get());
    }

    @Test
    public void singleDoesNotRequestMoreThanItNeedsToEmitErrorFromEmpty() {
        final AtomicLong request = new AtomicLong();
        try {
            Flowable.empty().doOnRequest(new LongConsumer() {
                @Override
                public void accept(long n) {
                    request.addAndGet(n);
                }
            }).blockingSingle();
        } catch (NoSuchElementException e) {
            // FIXME single now triggers fast-path
            assertEquals(Long.MAX_VALUE, request.get());
        }
    }

    @Test
    public void singleDoesNotRequestMoreThanItNeedsToEmitErrorFromMoreThanOne() {
        final AtomicLong request = new AtomicLong();
        try {
            Flowable.just(1, 2).doOnRequest(new LongConsumer() {
                @Override
                public void accept(long n) {
                    request.addAndGet(n);
                }
            }).blockingSingle();
        } catch (IllegalArgumentException e) {
            // FIXME single now triggers fast-path
            assertEquals(Long.MAX_VALUE, request.get());
        }
    }

    @Test
    public void singleWithPredicate() {
        Maybe<Integer> maybe = Flowable.just(1, 2)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        maybe.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateAndTooManyElements() {
        Maybe<Integer> maybe = Flowable.just(1, 2, 3, 4)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        maybe.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateAndEmpty() {
        Maybe<Integer> maybe = Flowable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        maybe.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefault() {
        Single<Integer> single = Flowable.just(1).single(2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithTooManyElements() {
        Single<Integer> single = Flowable.just(1, 2).single(3);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithEmpty() {
        Single<Integer> single = Flowable.<Integer> empty()
                .single(1);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicate() {
        Single<Integer> single = Flowable.just(1, 2)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateAndTooManyElements() {
        Single<Integer> single = Flowable.just(1, 2, 3, 4)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateAndEmpty() {
        Single<Integer> single = Flowable.just(1)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void issue1527() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Maybe<Integer> reduced = source.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 + i2;
            }
        });

        Integer r = reduced.blockingGet();
        assertEquals(21, r.intValue());
    }

    @Test
    public void singleOrErrorNoElement() {
        Flowable.empty()
            .singleOrError()
            .test()
            .assertNoValues()
            .assertError(NoSuchElementException.class);
    }

    @Test
    public void singleOrErrorOneElement() {
        Flowable.just(1)
            .singleOrError()
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void singleOrErrorMultipleElements() {
        Flowable.just(1, 2, 3)
            .singleOrError()
            .test()
            .assertNoValues()
            .assertError(IllegalArgumentException.class);
    }

    @Test
    public void singleOrErrorError() {
        Flowable.error(new RuntimeException("error"))
            .singleOrError()
            .to(TestHelper.testConsumer())
            .assertNoValues()
            .assertErrorMessage("error")
            .assertError(RuntimeException.class);
    }

    @Test
    public void issue1527Flowable() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Flowable<Integer> reduced = source.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 + i2;
            }
        }).toFlowable();

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
    }

    @Test
    public void singleElementOperatorDoNotSwallowExceptionWhenDone() {
        final Throwable exception = new RuntimeException("some error");
        final AtomicReference<Throwable> error = new AtomicReference<>();

        try {
            RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
                @Override public void accept(final Throwable throwable) throws Exception {
                    error.set(throwable);
                }
            });

            Flowable.unsafeCreate(new Publisher<Integer>() {
                @Override public void subscribe(final Subscriber<? super Integer> subscriber) {
                    subscriber.onComplete();
                    subscriber.onError(exception);
                }
            }).singleElement().test().assertComplete();

            assertSame(exception, error.get().getCause());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> f) throws Exception {
                return f.singleOrError();
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> f) throws Exception {
                return f.singleElement();
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> f) throws Exception {
                return f.singleOrError().toFlowable();
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowableToSingle(new Function<Flowable<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Flowable<Object> f) throws Exception {
                return f.singleOrError();
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.singleOrError().toFlowable();
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowableToMaybe(new Function<Flowable<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Flowable<Object> f) throws Exception {
                return f.singleElement();
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.singleElement().toFlowable();
            }
        });
    }

    @Test
    public void cancelAsFlowable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.singleOrError().toFlowable().test();

        assertTrue(pp.hasSubscribers());

        ts.assertEmpty();

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void singleOrError() {
        Flowable.empty()
        .singleOrError()
        .toFlowable()
        .test()
        .assertFailure(NoSuchElementException.class);
    }
}

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

package io.reactivex.internal.operators.observable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.PublishSubject;

public class ObservableSampleTest {
    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private Observer<Long> observer;
    private Observer<Object> observer2;

    @Before
    // due to mocking
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        observer = TestHelper.mockObserver();
        observer2 = TestHelper.mockObserver();
    }

    @Test
    public void testSample() {
        Observable<Long> source = Observable.unsafeCreate(new ObservableSource<Long>() {
            @Override
            public void subscribe(final Observer<? super Long> observer1) {
                observer1.onSubscribe(Disposables.empty());
                innerScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        observer1.onNext(1L);
                    }
                }, 1, TimeUnit.SECONDS);
                innerScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        observer1.onNext(2L);
                    }
                }, 2, TimeUnit.SECONDS);
                innerScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        observer1.onComplete();
                    }
                }, 3, TimeUnit.SECONDS);
            }
        });

        Observable<Long> sampled = source.sample(400L, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(800L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(any(Long.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1200L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1600L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, never()).onNext(2L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerNormal() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onNext(2);
        sampler.onNext(1);
        source.onNext(3);
        source.onNext(4);
        sampler.onNext(2);
        source.onComplete();
        sampler.onNext(3);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, never()).onNext(1);
        inOrder.verify(observer2, times(1)).onNext(2);
        inOrder.verify(observer2, never()).onNext(3);
        inOrder.verify(observer2, times(1)).onNext(4);
        inOrder.verify(observer2, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerNoDuplicates() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onNext(2);
        sampler.onNext(1);
        sampler.onNext(1);

        source.onNext(3);
        source.onNext(4);
        sampler.onNext(2);
        sampler.onNext(2);

        source.onComplete();
        sampler.onNext(3);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, never()).onNext(1);
        inOrder.verify(observer2, times(1)).onNext(2);
        inOrder.verify(observer2, never()).onNext(3);
        inOrder.verify(observer2, times(1)).onNext(4);
        inOrder.verify(observer2, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerTerminatingEarly() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onNext(2);
        sampler.onNext(1);
        sampler.onComplete();

        source.onNext(3);
        source.onNext(4);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, never()).onNext(1);
        inOrder.verify(observer2, times(1)).onNext(2);
        inOrder.verify(observer2, times(1)).onComplete();
        inOrder.verify(observer2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerEmitAndTerminate() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onNext(2);
        sampler.onNext(1);
        source.onNext(3);
        source.onComplete();
        sampler.onNext(2);
        sampler.onComplete();

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, never()).onNext(1);
        inOrder.verify(observer2, times(1)).onNext(2);
        inOrder.verify(observer2, never()).onNext(3);
        inOrder.verify(observer2, times(1)).onComplete();
        inOrder.verify(observer2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerEmptySource() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onComplete();
        sampler.onNext(1);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onComplete();
        verify(observer2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onError(new RuntimeException("Forced failure!"));
        sampler.onNext(1);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onError(any(Throwable.class));
        verify(observer2, never()).onNext(any());
        verify(observer, never()).onComplete();
    }

    @Test
    public void sampleWithSamplerThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> sampler = PublishSubject.create();

        Observable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        sampler.onNext(1);
        sampler.onError(new RuntimeException("Forced failure!"));

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onNext(1);
        inOrder.verify(observer2, times(1)).onError(any(RuntimeException.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void testSampleUnsubscribe() {
        final Disposable s = mock(Disposable.class);
        Observable<Integer> o = Observable.unsafeCreate(
                new ObservableSource<Integer>() {
                    @Override
                    public void subscribe(Observer<? super Integer> observer) {
                        observer.onSubscribe(s);
                    }
                }
        );
        o.throttleLast(1, TimeUnit.MILLISECONDS).subscribe().dispose();
        verify(s).dispose();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().sample(1, TimeUnit.SECONDS, new TestScheduler()));

        TestHelper.checkDisposed(PublishSubject.create().sample(Observable.never()));
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .sample(1, TimeUnit.SECONDS)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emitLastTimed() {
        Observable.just(1)
        .sample(1, TimeUnit.DAYS, true)
        .test()
        .assertResult(1);
    }

    @Test
    public void emitLastTimedEmpty() {
        Observable.empty()
        .sample(1, TimeUnit.DAYS, true)
        .test()
        .assertResult();
    }

    @Test
    public void emitLastTimedCustomScheduler() {
        Observable.just(1)
        .sample(1, TimeUnit.DAYS, Schedulers.single(), true)
        .test()
        .assertResult(1);
    }

    @Test
    public void emitLastTimedRunCompleteRace() {
        for (int i = 0; i < 1000; i++) {
            final TestScheduler scheduler = new TestScheduler();

            final PublishSubject<Integer> pp = PublishSubject.create();

            TestObserver<Integer> ts = pp.sample(1, TimeUnit.SECONDS, scheduler, true)
            .test();

            pp.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }

    @Test
    public void emitLastOther() {
        Observable.just(1)
        .sample(Observable.timer(1, TimeUnit.DAYS), true)
        .test()
        .assertResult(1);
    }

    @Test
    public void emitLastOtherEmpty() {
        Observable.empty()
        .sample(Observable.timer(1, TimeUnit.DAYS), true)
        .test()
        .assertResult();
    }

    @Test
    public void emitLastOtherRunCompleteRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishSubject<Integer> pp = PublishSubject.create();
            final PublishSubject<Integer> sampler = PublishSubject.create();

            TestObserver<Integer> ts = pp.sample(sampler, true)
            .test();

            pp.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    sampler.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }

    @Test
    public void emitLastOtherCompleteCompleteRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishSubject<Integer> pp = PublishSubject.create();
            final PublishSubject<Integer> sampler = PublishSubject.create();

            TestObserver<Integer> ts = pp.sample(sampler, true).test();

            pp.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    sampler.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }

}

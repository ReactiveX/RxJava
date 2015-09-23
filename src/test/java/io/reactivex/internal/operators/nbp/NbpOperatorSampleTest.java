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

package io.reactivex.internal.operators.nbp;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.nbp.NbpPublishSubject;

public class NbpOperatorSampleTest {
    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private NbpSubscriber<Long> NbpObserver;
    private NbpSubscriber<Object> observer2;

    @Before
    // due to mocking
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        NbpObserver = TestHelper.mockNbpSubscriber();
        observer2 = TestHelper.mockNbpSubscriber();
    }

    @Test
    public void testSample() {
        NbpObservable<Long> source = NbpObservable.create(new NbpOnSubscribe<Long>() {
            @Override
            public void accept(final NbpSubscriber<? super Long> observer1) {
                observer1.onSubscribe(EmptyDisposable.INSTANCE);
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

        NbpObservable<Long> sampled = source.sample(400L, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(800L, TimeUnit.MILLISECONDS);
        verify(NbpObserver, never()).onNext(any(Long.class));
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1200L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(1L);
        verify(NbpObserver, never()).onNext(2L);
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1600L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(1L);
        verify(NbpObserver, never()).onNext(2L);
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(1L);
        inOrder.verify(NbpObserver, times(1)).onNext(2L);
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3000L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(1L);
        inOrder.verify(NbpObserver, never()).onNext(2L);
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerNormal() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> sampler = NbpPublishSubject.create();

        NbpObservable<Integer> m = source.sample(sampler);
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
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerNoDuplicates() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> sampler = NbpPublishSubject.create();

        NbpObservable<Integer> m = source.sample(sampler);
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
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerTerminatingEarly() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> sampler = NbpPublishSubject.create();

        NbpObservable<Integer> m = source.sample(sampler);
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
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerEmitAndTerminate() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> sampler = NbpPublishSubject.create();

        NbpObservable<Integer> m = source.sample(sampler);
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
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerEmptySource() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> sampler = NbpPublishSubject.create();

        NbpObservable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onComplete();
        sampler.onNext(1);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onComplete();
        verify(observer2, never()).onNext(any());
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void sampleWithSamplerSourceThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> sampler = NbpPublishSubject.create();

        NbpObservable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        source.onError(new RuntimeException("Forced failure!"));
        sampler.onNext(1);

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onError(any(Throwable.class));
        verify(observer2, never()).onNext(any());
        verify(NbpObserver, never()).onComplete();
    }

    @Test
    public void sampleWithSamplerThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> sampler = NbpPublishSubject.create();

        NbpObservable<Integer> m = source.sample(sampler);
        m.subscribe(observer2);

        source.onNext(1);
        sampler.onNext(1);
        sampler.onError(new RuntimeException("Forced failure!"));

        InOrder inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onNext(1);
        inOrder.verify(observer2, times(1)).onError(any(RuntimeException.class));
        verify(NbpObserver, never()).onComplete();
    }

    @Test
    public void testSampleUnsubscribe() {
        final Disposable s = mock(Disposable.class);
        NbpObservable<Integer> o = NbpObservable.create(
                new NbpOnSubscribe<Integer>() {
                    @Override
                    public void accept(NbpSubscriber<? super Integer> NbpSubscriber) {
                        NbpSubscriber.onSubscribe(s);
                    }
                }
        );
        o.throttleLast(1, TimeUnit.MILLISECONDS).subscribe().dispose();
        verify(s).dispose();
    }
}
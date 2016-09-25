/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.subjects;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.observers.*;

public class UnicastSubjectTest {

    @Test
    public void fusionLive() {
        UnicastSubject<Integer> ap = UnicastSubject.create();

        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC));

        ts.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onNext(1);

        ts.assertValue(1).assertNoErrors().assertNotComplete();

        ap.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void fusionOfflie() {
        UnicastSubject<Integer> ap = UnicastSubject.create();
        ap.onNext(1);
        ap.onComplete();

        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult(1);
    }

    @Test
    public void onTerminateCalledWhenOnError() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastSubject<Integer> us = UnicastSubject.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        assertEquals(false, didRunOnTerminate.get());
        us.onError(new RuntimeException("some error"));
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test
    public void onTerminateCalledWhenOnComplete() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastSubject<Integer> us = UnicastSubject.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        assertEquals(false, didRunOnTerminate.get());
        us.onComplete();
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test
    public void onTerminateCalledWhenCanceled() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastSubject<Integer> us = UnicastSubject.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        final Disposable subscribe = us.subscribe();

        assertEquals(false, didRunOnTerminate.get());
        subscribe.dispose();
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test(expected = NullPointerException.class)
    public void nullOnTerminate() {
        UnicastSubject.create(5, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCapacityHint() {
        UnicastSubject.create(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroCapacityHint() {
        UnicastSubject.create(0);
    }

    @Test
    public void onNextNull() {
        final UnicastSubject<Object> up = UnicastSubject.create();

        up.onNext(null);

        up.test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNull() {
        final UnicastSubject<Object> up = UnicastSubject.create();

        up.onError(null);

        up.test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }
}

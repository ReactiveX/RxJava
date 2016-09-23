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

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

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
}

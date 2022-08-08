/*
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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableThrottleLatestTest extends RxJavaTest {

    @Test
    public void just() {
        Observable.just(1)
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void range() {
        Observable.range(1, 5)
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void rangeEmitLatest() {
        Observable.range(1, 5)
        .throttleLatest(1, TimeUnit.MINUTES, true)
        .test()
        .assertResult(1, 5);
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> f) throws Exception {
                return f.throttleLatest(1, TimeUnit.MINUTES);
            }
        });
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(
                Observable.never()
                .throttleLatest(1, TimeUnit.MINUTES)
        );
    }

    @Test
    public void normal() {
        TestScheduler sch = new TestScheduler();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch).test();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        ps.onNext(3);

        to.assertValuesOnly(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3);

        ps.onNext(4);

        to.assertValuesOnly(1, 3);

        ps.onNext(5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3, 5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3, 5);

        ps.onNext(6);

        to.assertValuesOnly(1, 3, 5, 6);

        ps.onNext(7);
        ps.onComplete();

        to.assertResult(1, 3, 5, 6);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertResult(1, 3, 5, 6);
    }

    @Test
    public void normalEmitLast() {
        TestScheduler sch = new TestScheduler();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch, true).test();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        ps.onNext(3);

        to.assertValuesOnly(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3);

        ps.onNext(4);

        to.assertValuesOnly(1, 3);

        ps.onNext(5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3, 5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3, 5);

        ps.onNext(6);

        to.assertValuesOnly(1, 3, 5, 6);

        ps.onNext(7);
        ps.onComplete();

        to.assertResult(1, 3, 5, 6, 7);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertResult(1, 3, 5, 6, 7);
    }

    @Test
    public void take() throws Throwable {
        Action onCancel = mock(Action.class);

        Observable.range(1, 5)
        .doOnDispose(onCancel)
        .throttleLatest(1, TimeUnit.MINUTES)
        .take(1)
        .test()
        .assertResult(1);

        verify(onCancel).run();
    }

    @Test
    public void reentrantComplete() {
        TestScheduler sch = new TestScheduler();
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(2);
                }
                if (t == 2) {
                    ps.onComplete();
                }
            }
        };

        ps.throttleLatest(1, TimeUnit.SECONDS, sch).subscribe(to);

        ps.onNext(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertResult(1, 2);
    }

    /** Emit 1, 2, 3, then advance time by a second; 1 and 3 should end up in downstream, 2 should be dropped. */
    @Test
    public void onDroppedBasicNoEmitLast() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        TestObserver<Object> drops = new TestObserver<>();
        drops.onSubscribe(Disposable.empty());

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test();

        to.assertEmpty();
        drops.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);
        drops.assertEmpty();

        ps.onNext(2);

        to.assertValuesOnly(1);
        drops.assertEmpty();

        ps.onNext(3);

        to.assertValuesOnly(1);
        drops.assertValuesOnly(2);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3);
        drops.assertValuesOnly(2);

        ps.onComplete();

        to.assertResult(1, 3);

        drops.assertValuesOnly(2);
    }

    /** Emit 1, 2, 3; 1 should end up in downstream, 2, 3 should be dropped. */
    @Test
    public void onDroppedBasicNoEmitLastDropLast() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        TestObserver<Object> drops = new TestObserver<>();
        drops.onSubscribe(Disposable.empty());

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test();

        to.assertEmpty();
        drops.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);
        drops.assertEmpty();

        ps.onNext(2);

        to.assertValuesOnly(1);
        drops.assertEmpty();

        ps.onNext(3);

        to.assertValuesOnly(1);
        drops.assertValuesOnly(2);

        ps.onComplete();

        to.assertResult(1);

        drops.assertValuesOnly(2, 3);
    }

    /** Emit 1, 2, 3; 1 and 3 should end up in downstream, 2 should be dropped. */
    @Test
    public void onDroppedBasicEmitLast() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        TestObserver<Object> drops = new TestObserver<>();
        drops.onSubscribe(Disposable.empty());

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch, true, drops::onNext)
        .test();

        to.assertEmpty();
        drops.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);
        drops.assertEmpty();

        ps.onNext(2);

        to.assertValuesOnly(1);
        drops.assertEmpty();

        ps.onNext(3);

        to.assertValuesOnly(1);
        drops.assertValuesOnly(2);

        ps.onComplete();

        to.assertResult(1, 3);

        drops.assertValuesOnly(2);
    }

    /** Emit 1, 2, 3; 3 should trigger an error to the downstream because 2 is dropped and the callback crashes. */
    @Test
    public void onDroppedBasicNoEmitLastFirstDropCrash() throws Throwable {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestObserver<Integer> to = ps
        .doOnDispose(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> {
            if (d == 2) {
                throw new TestException("forced");
            }
        })
        .test();

        to.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        ps.onNext(3);

        to.assertFailure(TestException.class, 1);

        verify(whenDisposed).run();
    }

    /**
     * Emit 1, 2, Error; the error should trigger the drop callback and crash it too,
     * downstream gets 1, composite(source, drop-crash).
     */
    @Test
    public void onDroppedBasicNoEmitLastOnErrorDropCrash() throws Throwable {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestObserverEx<Integer> to = ps
        .doOnDispose(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> { throw new TestException("forced " + d); })
        .subscribeWith(new TestObserverEx<>());

        to.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        ps.onError(new TestException("source"));

        to.assertFailure(CompositeException.class, 1);

        TestHelper.assertCompositeExceptions(to, TestException.class, "source", TestException.class, "forced 2");

        verify(whenDisposed, never()).run();
    }

    /**
     * Emit 1, 2, 3; 3 should trigger a drop-crash for 2, which then would trigger the error path and drop-crash for 3,
     * the last item not delivered, downstream gets 1, composite(drop-crash 2, drop-crash 3).
     */
    @Test
    public void onDroppedBasicEmitLastOnErrorDropCrash() throws Throwable {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestObserverEx<Integer> to = ps
        .doOnDispose(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, true, d -> { throw new TestException("forced " + d); })
        .subscribeWith(new TestObserverEx<>());

        to.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        ps.onNext(3);

        to.assertFailure(CompositeException.class, 1);

        TestHelper.assertCompositeExceptions(to, TestException.class, "forced 2", TestException.class, "forced 3");

        verify(whenDisposed).run();
    }

    /** Emit 1, complete; Downstream gets 1, complete, no drops. */
    @Test
    public void onDroppedBasicNoEmitLastNoLastToDrop() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        TestObserver<Object> drops = new TestObserver<>();
        drops.onSubscribe(Disposable.empty());

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test();

        to.assertEmpty();
        drops.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);
        drops.assertEmpty();

        ps.onComplete();

        to.assertResult(1);
        drops.assertEmpty();
    }

    /** Emit 1, error; Downstream gets 1, error, no drops. */
    @Test
    public void onDroppedErrorNoEmitLastNoLastToDrop() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        TestObserver<Object> drops = new TestObserver<>();
        drops.onSubscribe(Disposable.empty());

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test();

        to.assertEmpty();
        drops.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);
        drops.assertEmpty();

        ps.onError(new TestException());

        to.assertFailure(TestException.class, 1);
        drops.assertEmpty();
    }

    /**
     * Emit 1, 2, complete; complete should crash drop, downstream gets 1, drop-crash 2.
     */
    @Test
    public void onDroppedHasLastNoEmitLastDropCrash() throws Throwable {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestObserverEx<Integer> to = ps
        .doOnDispose(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> { throw new TestException("forced " + d); })
        .subscribeWith(new TestObserverEx<>());

        to.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        ps.onComplete();

        to.assertFailureAndMessage(TestException.class, "forced 2", 1);

        verify(whenDisposed, never()).run();
    }

    /**
     * Emit 1, 2 then dispose the sequence; downstream gets 1, drop should get for 2.
     */
    @Test
    public void onDroppedDisposeDrops() throws Throwable {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestObserver<Object> drops = new TestObserver<>();
        drops.onSubscribe(Disposable.empty());

        TestObserverEx<Integer> to = ps
        .doOnDispose(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .subscribeWith(new TestObserverEx<>());

        to.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        to.dispose();

        to.assertValuesOnly(1);
        drops.assertValuesOnly(2);

        verify(whenDisposed).run();
    }

    /**
     * Emit 1 then dispose the sequence; downstream gets 1, drop should not get called.
     */
    @Test
    public void onDroppedDisposeNoDrops() throws Throwable {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestObserver<Object> drops = new TestObserver<>();
        drops.onSubscribe(Disposable.empty());

        TestObserverEx<Integer> to = ps
        .doOnDispose(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .subscribeWith(new TestObserverEx<>());

        to.assertEmpty();

        ps.onNext(1);

        to.assertValuesOnly(1);

        to.dispose();

        to.assertValuesOnly(1);
        drops.assertEmpty();

        verify(whenDisposed).run();
    }

    /**
     * Emit 1, 2 then dispose the sequence; downstream gets 1, global error handler should get drop-crash 2.
     */
    @Test
    public void onDroppedDisposeCrashesDrop() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            PublishSubject<Integer> ps = PublishSubject.create();

            TestScheduler sch = new TestScheduler();

            Action whenDisposed = mock(Action.class);

            TestObserverEx<Integer> to = ps
            .doOnDispose(whenDisposed)
            .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> { throw new TestException("forced " + d); })
            .subscribeWith(new TestObserverEx<>());

            to.assertEmpty();

            ps.onNext(1);

            to.assertValuesOnly(1);

            ps.onNext(2);

            to.assertValuesOnly(1);

            to.dispose();

            to.assertValuesOnly(1);

            verify(whenDisposed).run();

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "forced 2");
        });
    }
}

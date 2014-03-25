/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static rx.operators.OperationToOperator.toOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Functions;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class OperationToOperatorTest {

    private static final int AWAIT_SECONDS = 10;

    @Test
    public void testUnsubscribeFromAsynchronousSource() throws InterruptedException {

        UnsubscribeDetector<Long> detector = UnsubscribeDetector.detect();
        Observable
        // every 100ms
                .interval(100, TimeUnit.MILLISECONDS)
                // detect unsubscribe
                .lift(detector)
                // use toOperator
                .lift(toOperator(Functions.<Observable<Long>> identity()))
                // just take one then unsubscribe
                .take(1)
                // block and get result
                .toBlockingObservable().single();
        // wait for expected unsubscription
        assertTrue(detector.latch().await(AWAIT_SECONDS, TimeUnit.SECONDS));

    }

    @Test
    public void testUnsubscribeFromSynchronousSource() throws InterruptedException {
        UnsubscribeDetector<Integer> detector = UnsubscribeDetector.detect();
        PublishSubject<Integer> subject = PublishSubject.create();
        subject
        // detect unsubscribe
        .lift(detector)
        // use toOperator
                .lift(toOperator(Functions.<Observable<Integer>> identity()))
                // get first only and unsubscribe
                .take(1)
                // subscribe and ignore events
                .subscribe();
        subject.onNext(1);
        // should have unsubscribed because of take(1)
        assertTrue(detector.latch().await(AWAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testMultipleNonSimultaeousSubscriptions() {
        Observable<Integer> sequence = Observable.range(1, 3).lift(
                toOperator(Functions.<Observable<Integer>> identity()));
        assertEquals(asList(1, 2, 3), sequence.toList().toBlockingObservable().single());
        assertEquals(asList(1, 2, 3), sequence.toList().toBlockingObservable().single());
    }

    @Test
    public void testMultipleSimultaneousSubscriptions() {
        PublishSubject<Integer> subject = PublishSubject.create();
        Recorder recorder1 = new Recorder();
        Recorder recorder2 = new Recorder();
        Observable<Integer> source = subject.lift(square);
        source.subscribe(recorder1);
        source.subscribe(recorder2);
        subject.onNext(1);
        assertEquals(asList(1), recorder1.list());
        assertEquals(asList(1), recorder2.list());
        subject.onNext(2);
        assertEquals(asList(1, 4), recorder1.list());
        assertEquals(asList(1, 4), recorder2.list());
        assertFalse(recorder1.isCompleted());
        assertFalse(recorder2.isCompleted());
        subject.onCompleted();
        assertTrue(recorder1.isCompleted());
        assertTrue(recorder2.isCompleted());
    }

    @Test
    public void testErrorsPassedThroughOperator() {
        PublishSubject<Integer> subject = PublishSubject.create();
        Recorder recorder1 = new Recorder();
        Recorder recorder2 = new Recorder();
        Observable<Integer> source = subject.lift(square);
        source.subscribe(recorder1);
        source.subscribe(recorder2);
        subject.onNext(1);
        assertEquals(asList(1), recorder1.list());
        assertEquals(asList(1), recorder2.list());
        subject.onNext(2);
        assertEquals(asList(1, 4), recorder1.list());
        assertEquals(asList(1, 4), recorder2.list());
        Exception e = new Exception("boo");
        assertTrue(recorder1.errors().isEmpty());
        assertTrue(recorder2.errors().isEmpty());
        subject.onError(e);
        assertEquals(asList(e), recorder1.errors());
        assertEquals(asList(e), recorder2.errors());
    }
    
    private static Func1<Observable<Integer>,Observable<Integer>> square = new Func1<Observable<Integer>,Observable<Integer>>(){

        @Override
        public Observable<Integer> call(Observable<Integer> source) {
            return source.map(new Func1<Integer,Integer>() {

                @Override
                public Integer call(Integer n) {
                    return n*n;
                }});
        }};

    private static class Recorder implements Observer<Integer> {

        private final List<Throwable> errors = new ArrayList<Throwable>();
        private final List<Integer> list = new ArrayList<Integer>();
        private boolean completed = false;

        boolean isCompleted() {
            return completed;
        }

        List<Throwable> errors() {
            return errors;
        }

        List<Integer> list() {
            return list;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        @Override
        public void onError(Throwable e) {
            errors.add(e);
        }

        @Override
        public void onNext(Integer t) {
            list.add(t);
        }

    }

    /**
     * Provides a {@link CountDownLatch} to assist with detecting unsubscribe
     * calls.
     * 
     * @param <T>
     */
    private static class UnsubscribeDetector<T> implements Operator<T, T> {

        private final CountDownLatch latch;

        /**
         * Constructor.
         */
        UnsubscribeDetector() {
            latch = new CountDownLatch(1);
        }

        @Override
        public Subscriber<? super T> call(Subscriber<? super T> subscriber) {
            subscriber.add(new Subscription() {

                private final AtomicBoolean subscribed = new AtomicBoolean(true);

                @Override
                public void unsubscribe() {
                    latch.countDown();
                    subscribed.set(false);
                }

                @Override
                public boolean isUnsubscribed() {
                    return subscribed.get();
                }
            });
            return subscriber;
        }

        /**
         * Returns a latch that will be at zero if one unsubscribe has occurred.
         * 
         * @return
         */
        CountDownLatch latch() {
            return latch;
        }

        /**
         * Factory method.
         * 
         * @return
         */
        static <T> UnsubscribeDetector<T> detect() {
            return new UnsubscribeDetector<T>();
        }
    }

}

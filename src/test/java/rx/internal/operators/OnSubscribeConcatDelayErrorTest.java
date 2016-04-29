/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import org.junit.Test;

import rx.Observable;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OnSubscribeConcatDelayErrorTest {

    @Test
    public void mainCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        source.concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);
        
        source.onNext(1);
        source.onNext(2);
        source.onCompleted();
        
        ts.assertValues(1, 2, 2, 3);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void mainErrors() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        source.concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);
        
        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());
        
        ts.assertValues(1, 2, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test
    public void innerErrors() {
        final Observable<Integer> inner = Observable.range(1, 2).concatWith(Observable.<Integer>error(new TestException()));
        
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.range(1, 3).concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return inner;
            }
        }).subscribe(ts);
        
        ts.assertValues(1, 2, 1, 2, 1, 2);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void singleInnerErrors() {
        final Observable<Integer> inner = Observable.range(1, 2).concatWith(Observable.<Integer>error(new TestException()));
        
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.just(1)
        .asObservable() // prevent scalar optimization
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return inner;
            }
        }).subscribe(ts);
        
        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerNull() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.just(1)
        .asObservable() // prevent scalar optimization
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return null;
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(NullPointerException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerThrows() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.just(1)
        .asObservable() // prevent scalar optimization
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                throw new TestException();
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerWithEmpty() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.range(1, 3)
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return v == 2 ? Observable.<Integer>empty() : Observable.range(1, 2);
            }
        }).subscribe(ts);
        
        ts.assertValues(1, 2, 1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void innerWithScalar() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.range(1, 3)
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return v == 2 ? Observable.just(3) : Observable.range(1, 2);
            }
        }).subscribe(ts);
        
        ts.assertValues(1, 2, 3, 1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        Observable.range(1, 3).concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(3);
        ts.assertValues(1, 2, 2, 3);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(2);
        
        ts.assertValues(1, 2, 2, 3, 3, 4);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

}

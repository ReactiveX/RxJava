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

import rx.*;
import rx.functions.Func2;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

/**
 * Test if an xWith operator chained repeatedly does not cause StackOverflowError.
 */
public class OperatorXWithTest {
    
    final int n = 5000;
    
    @Test
    public void mergeWithObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable<Integer> source = Observable.just(1);
        Observable<Integer> result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.mergeWith(source);
        }
        
        result.subscribe(ts);
        
        ts.assertValueCount(n + 1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void concatWithObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable<Integer> source = Observable.just(1);
        Observable<Integer> result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.concatWith(source);
        }
        
        result.subscribe(ts);
        
        ts.assertValueCount(n + 1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void ambWithObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        PublishSubject<Integer> source = PublishSubject.create();
        Observable<Integer> result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.ambWith(source);
        }
        
        result.subscribe(ts);
        
        source.onNext(1);
        source.onCompleted();
        
        ts.assertValueCount(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void startWithObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable<Integer> source = Observable.just(1);
        Observable<Integer> result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.startWith(source);
        }
        
        result.subscribe(ts);
        
        ts.assertValueCount(n + 1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void zipWithObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Func2<Integer, Integer, Integer> f = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }
        };
        
        Observable<Integer> source = Observable.just(1);
        Observable<Integer> result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.zipWith(source, f);
        }
        
        result.subscribe(ts);
        
        ts.assertValue(n + 1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

// TODO There is no Single.ambWith yet
//    @Test
//    public void ambWithSingle() {
//        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
//        
//        PublishSubject<Integer> source = PublishSubject.create();
//        Single<Integer> result = source.toSingle();
//        
//        for (int i = 0; i < n; i++) {
//            result = result.ambWith(source);
//        }
//        
//        result.subscribe(ts);
//        
//        source.onNext(1);
//        source.onCompleted();
//        
//        ts.assertValueCount(1);
//        ts.assertNoErrors();
//        ts.assertCompleted();
//    }
    
    @Test
    public void zipWithSingle() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Func2<Integer, Integer, Integer> f = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }
        };
        
        Single<Integer> source = Single.just(1);
        Single<Integer> result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.zipWith(source, f);
        }
        
        result.subscribe(ts);
        
        ts.assertValue(n + 1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void mergeWithCompletable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Completable source = Completable.complete();
        Completable result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.mergeWith(source);
        }
        
        result.subscribe(ts);
        
        ts.assertValueCount(0);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void concatWithCompletable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Completable source = Completable.complete();
        Completable result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.concatWith(source);
        }
        
        result.subscribe(ts);
        
        ts.assertValueCount(0);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void ambWithCompletable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        PublishSubject<Integer> source = PublishSubject.create();
        Completable result = source.toCompletable();
        
        for (int i = 0; i < n; i++) {
            result = result.ambWith(source.toCompletable());
        }
        
        result.subscribe(ts);
        
        source.onNext(1);
        source.onCompleted();
        
        ts.assertValueCount(0);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void startWithCompletable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Completable source = Completable.complete();
        Completable result = source;
        
        for (int i = 0; i < n; i++) {
            result = result.startWith(source);
        }
        
        result.subscribe(ts);
        
        ts.assertValueCount(0);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
}

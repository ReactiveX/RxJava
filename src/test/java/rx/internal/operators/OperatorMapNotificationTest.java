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

package rx.internal.operators;

import org.junit.Test;

import rx.Observable;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorMapNotificationTest {
    @Test
    public void testJust() {
        TestSubscriber<Object> ts = TestSubscriber.create();
        Observable.just(1)
        .flatMap(
                new Func1<Integer, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(Integer item) {
                        return Observable.just((Object)(item + 1));
                    }
                },
                new Func1<Throwable, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(Throwable e) {
                        return Observable.error(e);
                    }
                },
                new Func0<Observable<Object>>() {
                    @Override
                    public Observable<Object> call() {
                        return Observable.never();
                    }
                }
        ).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertValue(2);
    }
    
    @Test
    public void backpressure() {
        TestSubscriber<Object> ts = TestSubscriber.create(0L);

        Observable.range(1, 3).lift(new OperatorMapNotification<Integer, Integer>(
                new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer item) {
                        return item + 1;
                    }
                },
                new Func1<Throwable, Integer>() {
                    @Override
                    public Integer call(Throwable e) {
                        return 0;
                    }
                },
                new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return 5;
                    }
                }
        )).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(3);
        
        ts.assertValues(2, 3, 4);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);
        
        ts.assertValues(2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void noBackpressure() {
        TestSubscriber<Object> ts = TestSubscriber.create(0L);

        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.lift(new OperatorMapNotification<Integer, Integer>(
                new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer item) {
                        return item + 1;
                    }
                },
                new Func1<Throwable, Integer>() {
                    @Override
                    public Integer call(Throwable e) {
                        return 0;
                    }
                },
                new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return 5;
                    }
                }
        )).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ps.onNext(1);
        ps.onNext(2);
        ps.onNext(3);
        ps.onCompleted();
        
        ts.assertValues(2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

}

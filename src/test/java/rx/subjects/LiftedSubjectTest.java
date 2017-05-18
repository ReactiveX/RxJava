/**
 * Copyright 2016 Netflix, Inc.
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

package rx.subjects;

import org.junit.Test;

import rx.Observable.Operator;
import rx.functions.Func1;
import rx.internal.operators.OperatorMap;
import rx.observers.TestSubscriber;

public class LiftedSubjectTest {
    
    final Operator<Integer, String> parseInt = new OperatorMap<String, Integer>(new Func1<String, Integer>() {
        @Override
        public Integer call(String s) {
            return Integer.parseInt(s);
        }
    });
    
    final Operator<String, Integer> toString = new OperatorMap<Integer, String>(new Func1<Integer, String>() {
        @Override
        public String call(Integer s) {
            return String.valueOf(s + 1);
        }
    });
    
    void liftFront(Subject<Integer, Integer> actual) {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Subject<String, Integer> subject = actual.frontLift(parseInt);
        
        subject.subscribe(ts);
        
        subject.onNext("1");
        subject.onCompleted();
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    void liftBack(Subject<Integer, Integer> actual) {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        
        Subject<Integer, String> subject = actual.backLift(toString);
        
        subject.subscribe(ts);
        
        subject.onNext(1);
        subject.onCompleted();
        
        ts.assertValue("2");
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    void liftBoth(Subject<Integer, Integer> actual) {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        
        Subject<String, String> subject = actual.lift(parseInt, toString);
        
        subject.subscribe(ts);
        
        subject.onNext("1");
        subject.onCompleted();
        
        ts.assertValue("2");
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void asyncFront() {
        AsyncSubject<Integer> actual = AsyncSubject.create();
        liftFront(actual);
    }
    
    @Test
    public void asyncBack() {
        AsyncSubject<Integer> actual = AsyncSubject.create();
        liftBack(actual);
    }
    
    @Test
    public void asyncBoth() {
        AsyncSubject<Integer> actual = AsyncSubject.create();
        liftBoth(actual);
    }
    
    @Test
    public void publishFront() {
        PublishSubject<Integer> actual = PublishSubject.create();
        liftFront(actual);
    }
    
    @Test
    public void publishBack() {
        PublishSubject<Integer> actual = PublishSubject.create();
        liftBack(actual);
    }
    
    @Test
    public void publishBoth() {
        PublishSubject<Integer> actual = PublishSubject.create();
        liftBoth(actual);
    }
    
    @Test
    public void replayFront() {
        ReplaySubject<Integer> actual = ReplaySubject.create();
        liftFront(actual);
    }
    
    @Test
    public void replayBack() {
        ReplaySubject<Integer> actual = ReplaySubject.create();
        liftBack(actual);
    }
    
    @Test
    public void replayBoth() {
        ReplaySubject<Integer> actual = ReplaySubject.create();
        liftBoth(actual);
    }
    
    @Test
    public void behaviorFront() {
        BehaviorSubject<Integer> actual = BehaviorSubject.create();
        liftFront(actual);
    }
    
    @Test
    public void behaviorBack() {
        BehaviorSubject<Integer> actual = BehaviorSubject.create();
        liftBack(actual);
    }
    
    @Test
    public void behaviorBoth() {
        BehaviorSubject<Integer> actual = BehaviorSubject.create();
        liftBoth(actual);
    }

}

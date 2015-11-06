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

import java.io.IOException;

import org.junit.Test;

import rx.Observable;
import rx.exceptions.*;
import rx.functions.Func1E;
import rx.observers.TestSubscriber;

public class OnSubscribeMapIOTest {
    final Func1E<Integer, Integer, Exception> ADD_ONE = new Func1E<Integer, Integer, Exception>() {
        @Override
        public Integer call(Integer a) throws Exception {
            return a + 1;
        }
    };

    final Func1E<Integer, Integer, Exception> TRHOW_CHECKED = new Func1E<Integer, Integer, Exception>() {
        @Override
        public Integer call(Integer a) throws Exception {
            if (a == 2) {
                throw new IOException();
            }
            return a + 1;
        }
    };

    final Func1E<Integer, Integer, Exception> TRHOW_UNCHECKED = new Func1E<Integer, Integer, Exception>() {
        @Override
        public Integer call(Integer a) throws Exception {
            if (a == 2) {
                throw new TestException();
            }
            return a + 1;
        }
    };

    final Func1E<Integer, Integer, Exception> TRHOW_FATAL = new Func1E<Integer, Integer, Exception>() {
        @Override
        public Integer call(Integer a) throws Exception {
            if (a == 2) {
                throw new OnErrorNotImplementedException(new IOException());
            }
            return a + 1;
        }
    };

    @Test
    public void testSimple() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(1, 10).mapIO(ADD_ONE).subscribe(ts);
        
        ts.assertValues(2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void testSimpleBackpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(3);
        
        Observable.range(1, 10).mapIO(ADD_ONE).subscribe(ts);
        
        ts.assertValues(2, 3, 4);
        ts.assertNotCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void testThrowsChecked() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(1, 10).mapIO(TRHOW_CHECKED).subscribe(ts);
        
        ts.assertValue(2);
        ts.assertError(IOException.class);
        ts.assertNotCompleted();
        
    }
    @Test
    public void testThrowsUnchecked() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(1, 10).mapIO(TRHOW_UNCHECKED).subscribe(ts);
        
        ts.assertValue(2);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
        
    }
    
    @Test(expected = OnErrorNotImplementedException.class)
    public void testFatalBubblesUp() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(1, 10).mapIO(TRHOW_FATAL).subscribe(ts);
    }

}

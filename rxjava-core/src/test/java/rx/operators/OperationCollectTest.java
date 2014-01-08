/**
 * Copyright 2013 Netflix, Inc.
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import static org.junit.Assert.*;
import org.junit.Test;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public class OperationCollectTest {
    @Test
    public void testChunkify() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        Iterable<List<Integer>> iterable = source.toBlockingObservable().chunkify();

        Iterator<List<Integer>> it = iterable.iterator();
        
        assertEquals(Arrays.<Integer>asList(), it.next());
        
        source.onNext(1);

        assertEquals(Arrays.asList(1), it.next());

        source.onNext(2);
        source.onNext(3);

        assertEquals(Arrays.asList(2, 3), it.next());

        assertEquals(Arrays.<Integer>asList(), it.next());
        
        source.onNext(4);
        source.onNext(5);
        source.onNext(6);
        
        it.hasNext();

        source.onNext(7);
        source.onNext(8);
        source.onNext(9);
        source.onNext(10);
        
        it.hasNext();
        assertEquals(Arrays.asList(4, 5, 6), it.next());
        
        assertEquals(Arrays.asList(7, 8, 9, 10), it.next());
        
        source.onCompleted();

        assertEquals(Arrays.<Integer>asList(), it.next());

        try {
            it.next();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException ex) {
            // this is fine
        }
    }
    @Test
    public void testIterateManyTimes() {
        Observable<Integer> source = Observable.from(1, 2, 3);
        
        Iterable<List<Integer>> iter = source.toBlockingObservable().chunkify();
        
        for (int i = 0; i < 3; i++) {
            Iterator<List<Integer>> it = iter.iterator();
            
            assertTrue(it.hasNext());
         
            List<Integer> list = it.next();
            
            assertEquals(Arrays.asList(1, 2, 3), list);
        }
    }
    static final class CustomException extends RuntimeException {
        public CustomException(String message) {
            super(message);
        }
    }
    @Test
    public void testInitialBufferThrows() {
        Observable<Integer> source = Observable.from(1, 2, 3);
        
        Func0<Integer> initialBuffer = new Func0<Integer>() {
            @Override
            public Integer call() {
                throw new CustomException("Forced failure!");
            }
        };
        
        Func2<Integer, Integer, Integer> collector = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }
        };
        
        Func1<Integer, Integer> replaceBuffer = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return 0;
            }
            
        };
        
        Iterable<Integer> iter = source.toBlockingObservable().collect(initialBuffer, collector, replaceBuffer);
        
        Iterator<Integer> it = iter.iterator();
        
        try {
            it.next();
            fail("Failed to throw CustomException");
        } catch (CustomException ex) {
            // okay to get here
            
        }
    }
    
    @Test
    public void testCollectorThrows() {
        Observable<Integer> source = Observable.from(1, 2, 3);
        
        Func0<Integer> initialBuffer = new Func0<Integer>() {
            @Override
            public Integer call() {
                return 0;
            }
        };
        
        Func2<Integer, Integer, Integer> collector = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                throw new CustomException("Forced failure!");
            }
        };
        
        Func1<Integer, Integer> replaceBuffer = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return 0;
            }
            
        };
        
        Iterable<Integer> iter = source.toBlockingObservable().collect(initialBuffer, collector, replaceBuffer);
        
        Iterator<Integer> it = iter.iterator();
        
        try {
            it.next();
            fail("Failed to throw CustomException");
        } catch (CustomException ex) {
            // okay to get here
        }
    }
    @Test
    public void testReplaceBufferThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        Func0<Integer> initialBuffer = new Func0<Integer>() {
            @Override
            public Integer call() {
                return 0;
            }
        };
        
        Func2<Integer, Integer, Integer> collector = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }
        };
        
        Func1<Integer, Integer> replaceBuffer = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                throw new CustomException("Forced failure!");
            }
            
        };
        
        Iterable<Integer> iter = source.toBlockingObservable().collect(initialBuffer, collector, replaceBuffer);
        
        Iterator<Integer> it = iter.iterator();
        
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        
        try {
            it.next();
            fail("Failed to throw CustomException");
        } catch (CustomException ex) {
            // okay to get here
        }
    }
}

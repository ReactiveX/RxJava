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
package rx.exceptions;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class ExceptionsTest {

    @Test(expected = OnErrorNotImplementedException.class)
    public void testOnErrorNotImplementedIsThrown() {
        Observable.from(1, 2, 3).subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer t1) {
                throw new RuntimeException("hello");
            }

        });
    }

    @Test(expected = StackOverflowError.class)
    public void testStackOverflowIsThrown() {
        final PublishSubject<Integer> a = PublishSubject.create();
        final PublishSubject<Integer> b = PublishSubject.create();
        new Observer<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(Integer args) {
                System.out.println(args);
            }
        };
        a.subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(Integer args) {
                System.out.println(args);
            }
        });
        b.subscribe();
        a.subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(Integer args) {
                b.onNext(args + 1);
            }
        });
        b.subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(Integer args) {
                a.onNext(args + 1);
            }
        });
        a.onNext(1);
    }

    @Test(expected = ThreadDeath.class)
    public void testThreadDeathIsThrown() {
        Observable.from(1).subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(Integer t) {
                throw new ThreadDeath();
            }

        });
    }

    /**
     * https://github.com/Netflix/RxJava/issues/969
     */
    @Test
    public void testOnErrorExceptionIsThrown() {
        try {
            Observable.error(new IllegalArgumentException("original exception")).subscribe(new Observer<Object>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    throw new IllegalStateException("This should be thrown");
                }

                @Override
                public void onNext(Object o) {

                }
            });
            fail("expecting an exception to be thrown");
        } catch (CompositeException t) {
            CompositeException ce = (CompositeException) t;
            assertTrue(ce.getExceptions().get(0) instanceof IllegalArgumentException);
            assertTrue(ce.getExceptions().get(1) instanceof IllegalStateException);
        }
    }

}

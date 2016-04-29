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

package rx;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.*;

import org.junit.Test;

import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.operators.OperatorFilter;
import rx.internal.operators.OperatorMap;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class ObservableConversionTest {
    
    public static class Cylon {}
    
    public static class Jail {
        Object cylon;
        
        Jail(Object cylon) {
            this.cylon = cylon;
        }
    }
    
    public static class CylonDetectorObservable<T> {
        protected OnSubscribe<T> onSubscribe;
        
        public static <T> CylonDetectorObservable<T> create(OnSubscribe<T> onSubscribe) {
            return new CylonDetectorObservable<T>(onSubscribe);
        }

        protected CylonDetectorObservable(OnSubscribe<T> onSubscribe) {
            this.onSubscribe = onSubscribe;
        }

        public void subscribe(Subscriber<T> subscriber) {
            onSubscribe.call(subscriber);
        }

        public <R> CylonDetectorObservable<R> lift(Operator<? extends R, ? super T> operator) {
            return x(new RobotConversionFunc<T, R>(operator));
        }
        
        public <R, O> O x(Func1<OnSubscribe<T>, O> operator) {
            return operator.call(onSubscribe);
        }

        public <R> CylonDetectorObservable<? extends R> compose(Func1<CylonDetectorObservable<? super T>, CylonDetectorObservable<? extends R>> transformer) {
            return transformer.call(this);
        }
        
        public final CylonDetectorObservable<T> beep(Func1<? super T, Boolean> predicate) {
            return lift(new OperatorFilter<T>(predicate));
        }
        
        public final <R> CylonDetectorObservable<R> boop(Func1<? super T, ? extends R> func) {
            return lift(new OperatorMap<T, R>(func));
        }

        public CylonDetectorObservable<String> DESTROY() {
            return boop(new Func1<T, String>() {
                @Override
                public String call(T t) {
                    Object cylon = ((Jail) t).cylon;
                    throwOutTheAirlock(cylon);
                    if (t instanceof Jail) {
                        String name = cylon.toString();
                        return "Cylon '" + name + "' has been destroyed";
                    }
                    else {
                        return "Cylon 'anonymous' has been destroyed";
                    }
                }});
        }
        
        private static void throwOutTheAirlock(Object cylon) {
            // ...
        }
    }
    
    public static class RobotConversionFunc<T, R> implements Func1<OnSubscribe<T>, CylonDetectorObservable<R>> {
        private Operator<? extends R, ? super T> operator;

        public RobotConversionFunc(Operator<? extends R, ? super T> operator) {
            this.operator = operator;
        }

        @Override
        public CylonDetectorObservable<R> call(final OnSubscribe<T> onSubscribe) {
            return CylonDetectorObservable.create(new OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> o) {
                    try {
                        Subscriber<? super T> st = operator.call(o);
                        try {
                            st.onStart();
                            onSubscribe.call(st);
                        } catch (OnErrorNotImplementedException e) {
                            throw e;
                        } catch (Throwable e) {
                            st.onError(e);
                        }
                    } catch (OnErrorNotImplementedException e) {
                        throw e;
                    } catch (Throwable e) {
                        o.onError(e);
                    }
                
                }});
        }
    }
    
    public static class ConvertToCylonDetector<T> implements Func1<OnSubscribe<T>, CylonDetectorObservable<T>> {
        @Override
        public CylonDetectorObservable<T> call(final OnSubscribe<T> onSubscribe) {
            return CylonDetectorObservable.create(onSubscribe);
        }
    }
    
    public static class ConvertToObservable<T> implements Func1<OnSubscribe<T>, Observable<T>> {
        @Override
        public Observable<T> call(final OnSubscribe<T> onSubscribe) {
            return Observable.create(onSubscribe);
        }
    }
    
    @Test
    public void testConversionBetweenObservableClasses() {
        final TestSubscriber<String> subscriber = new TestSubscriber<String>(new Subscriber<String>(){

            @Override
            public void onCompleted() {
                System.out.println("Complete");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("error: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onNext(String t) {
                System.out.println(t);
            }});
        List<Object> crewOfBattlestarGalactica = Arrays.asList(new Object[] {"William Adama", "Laura Roslin", "Lee Adama", new Cylon()});
        Observable.from(crewOfBattlestarGalactica)
            .extend(new ConvertToCylonDetector<Object>())
            .beep(new Func1<Object, Boolean>(){
                @Override
                public Boolean call(Object t) {
                    return t instanceof Cylon;
                }})
             .boop(new Func1<Object, Object>() {
                @Override
                public Jail call(Object cylon) {
                    return new Jail(cylon);
                }})
            .DESTROY()
            .x(new ConvertToObservable<String>())
            .reduce("Cylon Detector finished. Report:\n", new Func2<String, String, String>() {
                @Override
                public String call(String a, String n) {
                    return a + n + "\n";
                }})
            .subscribe(subscriber);
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
    }
    
    @Test
    public void testConvertToConcurrentQueue() {
        final AtomicReference<Throwable> thrown = new AtomicReference<Throwable>(null);
        final AtomicBoolean isFinished = new AtomicBoolean(false);
        ConcurrentLinkedQueue<? extends Integer> queue = Observable.range(0,5)
                .flatMap(new Func1<Integer, Observable<Integer>>(){
                    @Override
                    public Observable<Integer> call(final Integer i) {
                        return Observable.range(0, 5)
                                .observeOn(Schedulers.io())
                                .map(new Func1<Integer, Integer>(){
                                    @Override
                                    public Integer call(Integer k) {
                                        try {
                                            Thread.sleep(System.currentTimeMillis() % 100);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        return i + k;
                                    }});
                    }})
                    .extend(new Func1<OnSubscribe<Integer>, ConcurrentLinkedQueue<Integer>>() {
                        @Override
                        public ConcurrentLinkedQueue<Integer> call(OnSubscribe<Integer> onSubscribe) {
                            final ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<Integer>();
                            onSubscribe.call(new Subscriber<Integer>(){
                                @Override
                                public void onCompleted() {
                                    isFinished.set(true);
                                }
        
                                @Override
                                public void onError(Throwable e) {
                                    thrown.set(e);
                                }
        
                                @Override
                                public void onNext(Integer t) {
                                    q.add(t);
                                }});
                            return q;
                        }});
        
        int x = 0;
        while(!isFinished.get()) {
            Integer i = queue.poll();
            if (i != null) {
                x++;
                System.out.println(x + " item: " + i);
            }
        }
        assertEquals(null, thrown.get());
    }
}

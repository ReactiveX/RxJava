/**
 * Copyright 2015 Netflix, Inc.
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
package rx.internal.producers;

import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import rx.*;
import rx.Observable;
import rx.Observable.*;
import rx.Observer;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.*;
import rx.subscriptions.SerialSubscription;

public class ProducersTest {
    @Test
    public void testSingleNoBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        SingleProducer<Integer> sp = new SingleProducer<Integer>(ts, 1);
        ts.setProducer(sp);
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1));
    }
    @Test
    public void testSingleWithBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.requestMore(0);
        SingleProducer<Integer> sp = new SingleProducer<Integer>(ts, 1);
        ts.setProducer(sp);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        Assert.assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        ts.requestMore(2);
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1));
    }
    
    @Test
    public void testSingleDelayedNoBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        SingleDelayedProducer<Integer> sdp = new SingleDelayedProducer<Integer>(ts);
        ts.setProducer(sdp);

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        Assert.assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        sdp.setValue(1);
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1));

        sdp.setValue(2);
        
        ts.assertReceivedOnNext(Arrays.asList(1));
    }
    @Test
    public void testSingleDelayedWithBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.requestMore(0);
        SingleDelayedProducer<Integer> sdp = new SingleDelayedProducer<Integer>(ts);
        ts.setProducer(sdp);

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        Assert.assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        sdp.setValue(1);

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        Assert.assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        ts.requestMore(2);
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1));

        sdp.setValue(2);
        
        ts.assertReceivedOnNext(Arrays.asList(1));
    }
    
    @Test
    public void testQueuedValueNoBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        QueuedValueProducer<Integer> qvp = new QueuedValueProducer<Integer>(ts);
        ts.setProducer(qvp);
        
        qvp.offer(1);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1));

        qvp.offer(2);
        qvp.offer(3);
        qvp.offer(4);

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4));
    }
    @Test
    public void testQueuedValueWithBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.requestMore(0);
        QueuedValueProducer<Integer> qvp = new QueuedValueProducer<Integer>(ts);
        ts.setProducer(qvp);

        qvp.offer(1);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        
        qvp.offer(2);
        ts.requestMore(2);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2));
        
        ts.requestMore(2);

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2));
        
        qvp.offer(3);
        qvp.offer(4);
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4));
    }
    
    @Test
    public void testQueuedNoBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        QueuedProducer<Integer> qp = new QueuedProducer<Integer>(ts);
        ts.setProducer(qp);
        
        qp.offer(1);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1));

        qp.offer(2);
        qp.offer(3);
        qp.offer(4);

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4));
        
        qp.onCompleted();
        
        ts.assertTerminalEvent();
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4));
    }
    @Test
    public void testQueuedWithBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.requestMore(0);
        QueuedProducer<Integer> qp = new QueuedProducer<Integer>(ts);
        ts.setProducer(qp);

        qp.offer(1);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        
        qp.offer(2);
        ts.requestMore(2);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2));
        
        ts.requestMore(2);

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2));
        
        qp.offer(3);
        qp.offer(4);
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4));
        
        qp.onCompleted();
        ts.assertTerminalEvent();
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4));
    }
    
    @Test
    public void testArbiter() {
        Producer p1 = mock(Producer.class);
        Producer p2 = mock(Producer.class);
        
        ProducerArbiter pa = new ProducerArbiter();
        
        pa.request(100);
        
        pa.setProducer(p1);
        
        verify(p1).request(100);
        
        pa.produced(50);
        
        pa.setProducer(p2);
        
        verify(p2).request(50);
    }
    
    static final class TestProducer implements Producer {
        final Observer<Integer> child;
        public TestProducer(Observer<Integer> child) {
            this.child = child;
        }
        @Override
        public void request(long n) {
            child.onNext((int)n);
        }
    }
    
    @Test
    public void testObserverArbiterWithBackpressure() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.requestMore(0);
        final ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(ts);
        ts.setProducer(poa);

        
        poa.setProducer(new TestProducer(poa));

        ts.requestMore(1);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1));

        poa.setProducer(null);
        ts.requestMore(5);

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1));

        poa.setProducer(new TestProducer(poa));

        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 5));
        
        poa.onCompleted();
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 5));
    }
    static final class SwitchTimer<T> 
    implements OnSubscribe<T> {
        final List<Observable<? extends T>> sources;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;
        public SwitchTimer(
                Iterable<? extends Observable<? extends T>> sources, 
                long time, TimeUnit unit, Scheduler scheduler) {
            this.scheduler = scheduler;
            this.sources = new ArrayList<Observable<? extends T>>();
            this.time = time;
            this.unit = unit;
            for (Observable<? extends T> o : sources) {
                this.sources.add(o);
            }
        }
        @Override
        public void call(Subscriber<? super T> child) {
            final ProducerObserverArbiter<T> poa = 
                new ProducerObserverArbiter<T>(child);
             
            Scheduler.Worker w = scheduler.createWorker();
            child.add(w);
             
            child.setProducer(poa);
             
            final SerialSubscription ssub = new SerialSubscription();
            child.add(ssub);
             
            final int[] index = new int[1];
             
            w.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    final int idx = index[0]++;
                    if (idx >= sources.size()) {
                        poa.onCompleted();
                        return;
                    }
                    Subscriber<T> s = new Subscriber<T>() {
                        @Override
                        public void onNext(T t) {
                            poa.onNext(t);
                        }
                        @Override
                        public void onError(Throwable e) {
                            poa.onError(e);
                        }
                        @Override
                        public void onCompleted() {
                            if (idx + 1 == sources.size()) {
                                poa.onCompleted();
                            }
                        }
                        @Override
                        public void setProducer(Producer producer) {
                            poa.setProducer(producer);
                        }
                    };
         
                    ssub.set(s);
                    sources.get(idx).unsafeSubscribe(s);
                }
            }, time, time, unit);
        }
    }
    final Func1<Long, Long> plus(final long n) {
        return new Func1<Long, Long>() {
            @Override
            public Long call(Long t) {
                return t + n;
            }
        };
    }
    @Test
    public void testObserverArbiterAsync() {
        TestScheduler test = Schedulers.test();
        @SuppressWarnings("unchecked")
        List<Observable<Long>> timers = Arrays.asList(
            Observable.interval(100, 100, TimeUnit.MILLISECONDS, test),
            Observable.interval(100, 100, TimeUnit.MILLISECONDS, test)
                .map(plus(20)),
            Observable.interval(100, 100, TimeUnit.MILLISECONDS, test)
                .map(plus(40))
        );
         
        Observable<Long> source = Observable.create(
            new SwitchTimer<Long>(timers, 550, 
            TimeUnit.MILLISECONDS, test));
                 
        TestSubscriber<Long> ts = new TestSubscriber<Long>();
        ts.requestMore(100);
        source.subscribe(ts);
            
        test.advanceTimeBy(1, TimeUnit.MINUTES);
        
        ts.assertTerminalEvent();
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(0L, 1L, 2L, 3L, 4L,
                20L, 21L, 22L, 23L, 24L,
                40L, 41L, 42L, 43L, 44L));
    }
    
    @Test(timeout = 1000)
    public void testProducerObserverArbiterUnbounded() {
        Observable.range(0, Integer.MAX_VALUE)
        .lift(new Operator<Integer, Integer>() {
            @Override
            public Subscriber<? super Integer> call(Subscriber<? super Integer> t) {
                final ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(t);
                
                Subscriber<Integer> parent = new Subscriber<Integer>() {

                    @Override
                    public void onCompleted() {
                        poa.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        poa.onError(e);
                    }

                    @Override
                    public void onNext(Integer t) {
                        poa.onNext(t);
                    }
                    
                    
                    @Override
                    public void setProducer(Producer p) {
                        poa.setProducer(p);
                    }
                };
                
                t.add(parent);
                t.setProducer(poa);
                
                return parent;
            }
        }).subscribe(new TestSubscriber<Integer>() {
            int count;
            @Override
            public void onNext(Integer t) {
                if (++count == 2) {
                    unsubscribe();
                }
            }
        });
    }
}

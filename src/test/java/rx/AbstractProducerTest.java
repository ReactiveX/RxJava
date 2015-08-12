/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package rx;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import rx.Observable.ProducerState;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.subscriptions.BooleanSubscription;

public class AbstractProducerTest {
    @Test
    public void testJust() {
        Observable<Integer> source = Observable.create(new Action1<ProducerState<Integer, Void>>() {
            @Override
            public void call(ProducerState<Integer, Void> ps) {
                Subscriber<? super Integer> s = ps.subscriber();
                s.onNext(1);
                s.onCompleted();
                ps.unsubscribe();
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                requestMore(0);
            }
        };
        
        source.subscribe(ts);
        
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        ts.assertNoErrors();
        assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        ts.requestMore(10);
        
        ts.assertReceivedOnNext(Arrays.asList(1));
        ts.assertNoErrors();
        ts.assertTerminalEvent();
    }
    
    @Test
    public void testEmpty() {
        Observable<Integer> source = Observable.create(new Action1<ProducerState<Integer, Void>>() {
            @Override
            public void call(ProducerState<Integer, Void> ps) {
                Subscriber<? super Integer> s = ps.subscriber();
                s.onCompleted();
                ps.unsubscribe();
            }
        });
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                requestMore(0);
            }
        };
        
        source.subscribe(ts);
        
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        ts.assertNoErrors();
        assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        ts.requestMore(10);
        
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        ts.assertNoErrors();
        ts.assertTerminalEvent();
    }

    @Test
    public void testNever() {
        Observable<Integer> source = Observable.create(new Action1<ProducerState<Integer, Void>>() {
            @Override
            public void call(ProducerState<Integer, Void> ps) {
                ps.unsubscribe();
            }
        });
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.subscribe(ts);
        
        ts.assertReceivedOnNext(Collections.<Integer>emptyList());
        ts.assertNoErrors();
        assertTrue(ts.getOnCompletedEvents().isEmpty());
    }
    
    @Test
    public void testRange() {
        final int start = 1;
        final int count = 100;

        Observable<Integer> source = Observable.create(new Action1<ProducerState<Integer, Void>>() {
            int value = start;
            int remaining = count;
            @Override
            public void call(ProducerState<Integer, Void> ps) {
                Subscriber<? super Integer> s = ps.subscriber();
                long r = ps.requested();
                
                while (r > 0 && remaining > 0) {
                    s.onNext(value);
                    value++;
                    if (--remaining <= 0) {
                        s.onCompleted();
                        ps.unsubscribe();
                        break;
                    }
                    r = ps.produced(1);
                }
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                requestMore(0);
            }
        };
        
        source.subscribe(ts);
        
        ts.requestMore(10);
        
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        ts.assertNoErrors();
        assertTrue(ts.getOnCompletedEvents().isEmpty());

        ts.requestMore(100);

        assertEquals(100, ts.getOnNextEvents().size());
        ts.assertNoErrors();
        ts.assertTerminalEvent();
    }
    
    @Test
    public void testIterable() {
        final List<Integer> list = new ArrayList<Integer>();
        for (int i = 1; i <= 100; i++) {
            list.add(i);
        }
        Observable<Integer> source = Observable.create(
            new Action1<ProducerState<Integer, Iterator<Integer>>>() {
                @Override
                public void call(ProducerState<Integer, Iterator<Integer>> ps) {
                    boolean b = false;
                    long r = ps.requested();
                    Subscriber<? super Integer> s = ps.subscriber();
                    Iterator<Integer> it = ps.state();
                    
                    while (r > 0 && (b = it.hasNext())) {
                        s.onNext(it.next());
                        r = ps.produced(1);
                    }
                    if (!b) {
                        s.onCompleted();
                        ps.unsubscribe();
                    }
                }
            }, 
            new Func1<Subscriber<? super Integer>, Iterator<Integer>>() {
                @Override
                public Iterator<Integer> call(Subscriber<? super Integer> t1) {
                    return list.iterator();
                }
            }
        );
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                requestMore(0);
            }
        };
        
        source.subscribe(ts);

        ts.requestMore(10);
        
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        ts.assertNoErrors();
        assertTrue(ts.getOnCompletedEvents().isEmpty());

        ts.requestMore(100);

        ts.assertReceivedOnNext(list);
        ts.assertNoErrors();
        ts.assertTerminalEvent();
    }
    
    @Test
    public void testFirehose() {
        Observable<Integer> source = Observable.create(new Action1<ProducerState<Integer, Void>>() {
            @Override
            public void call(ProducerState<Integer, Void> t1) {
                Subscriber<? super Integer> s = t1.subscriber();
                if (t1.requested() != Long.MAX_VALUE) {
                    s.onError(new IllegalStateException("Long.MAX_VALUE expected: " + t1.requested()));
                    return;
                }
                int value = 0;
                for (;!t1.isUnsubscribed();) {
                    s.onNext(value++);
                }
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            int c = 0;
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (++c == 10) {
                    unsubscribe();
                }
            }
        };
        
        source.subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertTrue(ts.getOnCompletedEvents().isEmpty());
    }
    @Test
    public void testFirehoseFreeResource() {
        final BooleanSubscription bs = new BooleanSubscription();
        
        Observable<Integer> source = Observable.create(new Action1<ProducerState<Integer, Subscription>>() {
            @Override
            public void call(ProducerState<Integer, Subscription> t1) {
                Subscriber<? super Integer> s = t1.subscriber();
                if (t1.requested() != Long.MAX_VALUE) {
                    s.onError(new IllegalStateException("Long.MAX_VALUE expected: " + t1.requested()));
                    return;
                }
                int value = 0;
                for (;!t1.isUnsubscribed();) {
                    s.onNext(value++);
                }
            }
        },
        new Func1<Subscriber<? super Integer>, Subscription>() {
            @Override
            public Subscription call(Subscriber<? super Integer> t1) {
                return bs;
            }
        },
        new Action1<Subscription>() {
            @Override
            public void call(Subscription t1) {
                t1.unsubscribe();
            }
        }
        );
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            int c = 0;
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (++c == 10) {
                    unsubscribe();
                }
            }
        };
        
        source.subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        assertTrue(bs.isUnsubscribed());
    }
}

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

package rx.observables;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;

import rx.*;
import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observables.AbstractOnSubscribe.SubscriptionState;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

/**
 * Test if AbstractOnSubscribe adheres to the usual unsubscription and backpressure contracts.
 */
public class AbstractOnSubscribeTest {
    @Test
    public void testJust() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onNext(1);
                state.onCompleted();
            }
        };
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        aos.toObservable().subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1));
    }
    @Test
    public void testJustMisbehaving() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onNext(1);
                state.onNext(2);
                state.onCompleted();
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        aos.toObservable().subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
        verify(o).onError(any(IllegalStateException.class));
    }
    @Test
    public void testJustMisbehavingOnCompleted() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onNext(1);
                state.onCompleted();
                state.onCompleted();
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        aos.toObservable().subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
        verify(o).onError(any(IllegalStateException.class));
    }
    @Test
    public void testJustMisbehavingOnError() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onNext(1);
                state.onError(new TestException("Forced failure 1"));
                state.onError(new TestException("Forced failure 2"));
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        aos.toObservable().subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
        verify(o).onError(any(IllegalStateException.class));
    }
    @Test
    public void testEmpty() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onCompleted();
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        aos.toObservable().subscribe(o);
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onCompleted();
    }
    @Test
    public void testNever() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.stop();
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        aos.toObservable().subscribe(o);
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onCompleted();
    }

    @Test
    public void testThrows() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                throw new TestException("Forced failure");
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        aos.toObservable().subscribe(o);
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testError() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onError(new TestException("Forced failure"));
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        aos.toObservable().subscribe(o);
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o).onError(any(TestException.class));
        verify(o, never()).onCompleted();
    }
    @Test
    public void testRange() {
        final int start = 1;
        final int count = 100;
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                long calls = state.calls();
                if (calls <= count) {
                    state.onNext((int)calls + start);
                    if (calls == count) {
                        state.onCompleted();
                    }
                }
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        aos.toObservable().subscribe(o);
        
        verify(o, never()).onError(any(TestException.class));
        for (int i = start; i < start + count; i++) {
            inOrder.verify(o).onNext(i);
        }
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
    @Test
    public void testFromIterable() {
        int n = 100;
        final List<Integer> source = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            source.add(i);
        }
        
        AbstractOnSubscribe<Integer, Iterator<Integer>> aos = new AbstractOnSubscribe<Integer, Iterator<Integer>>() {
            @Override
            protected Iterator<Integer> onSubscribe(
                    Subscriber<? super Integer> subscriber) {
                return source.iterator();
            }
            @Override
            protected void next(SubscriptionState<Integer, Iterator<Integer>> state) {
                Iterator<Integer> it = state.state();
                if (it.hasNext()) {
                    state.onNext(it.next());
                }
                if (!it.hasNext()) {
                    state.onCompleted();
                }
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        aos.toObservable().subscribe(o);
        
        verify(o, never()).onError(any(TestException.class));
        for (int i = 0; i < n; i++) {
            inOrder.verify(o).onNext(i);
        }
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testPhased() {
        final int count = 100;
        AbstractOnSubscribe<String, Void> aos = new AbstractOnSubscribe<String, Void>() {
            @Override
            protected void next(SubscriptionState<String, Void> state) {
                long c = state.calls();
                switch (state.phase()) {
                case 0:
                    if (c < count) {
                        state.onNext("Beginning");
                        if (c == count - 1) {
                            state.advancePhase();
                        }
                    }
                    break;
                case 1:
                    state.onNext("Beginning");
                    state.advancePhase();
                    break;
                case 2:
                    state.onNext("Finally");
                    state.onCompleted();
                    state.advancePhase();
                    break;
                default:
                    throw new IllegalStateException("Wrong phase: " + state.phase());
                }
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        aos.toObservable().subscribe(o);
        
        verify(o, never()).onError(any(Throwable.class));
        inOrder.verify(o, times(count + 1)).onNext("Beginning");
        inOrder.verify(o).onNext("Finally");
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
    @Test
    public void testPhasedRetry() {
        final int count = 100;
        AbstractOnSubscribe<String, Void> aos = new AbstractOnSubscribe<String, Void>() {
            int calls;
            int phase;
            @Override
            protected void next(SubscriptionState<String, Void> state) {
                switch (phase) {
                case 0:
                    if (calls++ < count) {
                        state.onNext("Beginning");
                        state.onError(new TestException());
                    } else {
                        phase++;
                    }
                    break;
                case 1:
                    state.onNext("Beginning");
                    phase++;
                    break;
                case 2:
                    state.onNext("Finally");
                    state.onCompleted();
                    phase++;
                    break;
                default:
                    throw new IllegalStateException("Wrong phase: " + state.phase());
                }
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        aos.toObservable().retry(2 * count).subscribe(o);
        
        verify(o, never()).onError(any(Throwable.class));
        inOrder.verify(o, times(count + 1)).onNext("Beginning");
        inOrder.verify(o).onNext("Finally");
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
    @Test
    public void testInfiniteTake() {
        int count = 100;
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onNext((int)state.calls());
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        aos.toObservable().take(count).subscribe(o);
        
        verify(o, never()).onError(any(Throwable.class));
        for (int i = 0; i < 100; i++) {
            inOrder.verify(o).onNext(i);
        }
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
    @Test
    public void testInfiniteRequestSome() {
        int count = 100;
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onNext((int)state.calls());
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        TestSubscriber<Object> ts = new TestSubscriber<Object>(o) {
            @Override
            public void onStart() {
                requestMore(0); // don't start right away
            }
        };
        
        aos.toObservable().subscribe(ts);
        
        ts.requestMore(count);
        
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onCompleted();
        for (int i = 0; i < count; i++) {
            inOrder.verify(o).onNext(i);
        }
        inOrder.verifyNoMoreInteractions();
    }
    @Test
    public void testIndependentStates() {
        int count = 100;
        final ConcurrentHashMap<Object, Object> states = new ConcurrentHashMap<Object, Object>();
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                states.put(state, state);
                state.stop();
            }
        };
        Observable<Integer> source = aos.toObservable();
        for (int i = 0; i < count; i++) {
            source.subscribe();
        }
        
        assertEquals(count, states.size());
    }
    @Test(timeout = 3000)
    public void testSubscribeOn() {
        final int start = 1;
        final int count = 100;
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                long calls = state.calls();
                if (calls <= count) {
                    state.onNext((int)calls + start);
                    if (calls == count) {
                        state.onCompleted();
                    }
                }
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        TestSubscriber<Object> ts = new TestSubscriber<Object>(o);

        aos.toObservable().subscribeOn(Schedulers.newThread()).subscribe(ts);
        
        ts.awaitTerminalEvent();
        
        verify(o, never()).onError(any(Throwable.class));
        for (int i = 1; i <= count; i++) {
            inOrder.verify(o).onNext(i);
        }
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();

    }
    @Test(timeout = 10000)
    public void testObserveOn() {
        final int start = 1;
        final int count = 1000;
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                long calls = state.calls();
                if (calls <= count) {
                    state.onNext((int)calls + start);
                    if (calls == count) {
                        state.onCompleted();
                    }
                }
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        TestSubscriber<Object> ts = new TestSubscriber<Object>(o);

        aos.toObservable().observeOn(Schedulers.newThread()).subscribe(ts);
        
        ts.awaitTerminalEvent();
        
        verify(o, never()).onError(any(Throwable.class));
        verify(o, times(count + 1)).onNext(any(Integer.class));
        verify(o).onCompleted();
        
        for (int i = 0; i < ts.getOnNextEvents().size(); i++) {
            Object object = ts.getOnNextEvents().get(i);
            assertEquals(i + 1, object);
        }
    }
    @Test
    public void testMissingEmission() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Action1<SubscriptionState<Object, Void>> empty = Actions.empty();
        AbstractOnSubscribe.create(empty).toObservable().subscribe(o);
        
        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any(Object.class));
        verify(o).onError(any(IllegalStateException.class));
    }
    
    @Test
    public void testCanRequestInOnNext() {
        AbstractOnSubscribe<Integer, Void> aos = new AbstractOnSubscribe<Integer, Void>() {
            @Override
            protected void next(SubscriptionState<Integer, Void> state) {
                state.onNext(1);
                state.onCompleted();
            }
        };
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        aos.toObservable().subscribe(new Subscriber<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                exception.set(e);
            }

            @Override
            public void onNext(Integer t) {
                request(1);
            }
        });
        if (exception.get()!=null) {
            exception.get().printStackTrace();
        }
        assertNull(exception.get());
    }
}

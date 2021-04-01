/*
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.util.CrashingIterable;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableFromIterableTest extends RxJavaTest {

    @Test
    public void listIterable() {
        Observable<String> o = Observable.fromIterable(Arrays.<String> asList("one", "two", "three"));

        Observer<String> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    /**
     * This tests the path that can not optimize based on size so must use setProducer.
     */
    @Test
    public void rawIterable() {
        Iterable<String> it = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {

                    int i;

                    @Override
                    public boolean hasNext() {
                        return i < 3;
                    }

                    @Override
                    public String next() {
                        return String.valueOf(++i);
                    }

                    @Override
                    public void remove() {
                    }

                };
            }

        };
        Observable<String> o = Observable.fromIterable(it);

        Observer<String> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext("1");
        verify(observer, times(1)).onNext("2");
        verify(observer, times(1)).onNext("3");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void observableFromIterable() {
        Observable<String> o = Observable.fromIterable(Arrays.<String> asList("one", "two", "three"));

        Observer<String> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void noBackpressure() {
        Observable<Integer> o = Observable.fromIterable(Arrays.asList(1, 2, 3, 4, 5));

        TestObserverEx<Integer> to = new TestObserverEx<>();

        o.subscribe(to);

        to.assertValues(1, 2, 3, 4, 5);
        to.assertTerminated();
    }

    @Test
    public void subscribeMultipleTimes() {
        Observable<Integer> o = Observable.fromIterable(Arrays.asList(1, 2, 3));

        for (int i = 0; i < 10; i++) {
            TestObserver<Integer> to = new TestObserver<>();

            o.subscribe(to);

            to.assertValues(1, 2, 3);
            to.assertNoErrors();
            to.assertComplete();
        }
    }

    @Test
    public void doesNotCallIteratorHasNextMoreThanRequiredWithBackpressure() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    int count = 1;

                    @Override
                    public void remove() {
                        // ignore
                    }

                    @Override
                    public boolean hasNext() {
                        if (count > 1) {
                            called.set(true);
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                };
            }
        };
        Observable.fromIterable(iterable).take(1).subscribe();
        assertFalse(called.get());
    }

    @Test
    public void doesNotCallIteratorHasNextMoreThanRequiredFastPath() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                        // ignore
                    }

                    int count = 1;

                    @Override
                    public boolean hasNext() {
                        if (count > 1) {
                            called.set(true);
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                };
            }
        };
        Observable.fromIterable(iterable).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                // unsubscribe on first emission
                cancel();
            }
        });
        assertFalse(called.get());
    }

    @Test
    public void fusionWithConcatMap() {
        TestObserver<Integer> to = new TestObserver<>();

        Observable.fromIterable(Arrays.asList(1, 2, 3, 4)).concatMap(
        new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(to);

        to.assertValues(1, 2, 2, 3, 3, 4, 4, 5);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void iteratorThrows() {
        Observable.fromIterable(new CrashingIterable(1, 100, 100))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNext2Throws() {
        Observable.fromIterable(new CrashingIterable(100, 2, 100))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void hasNextCancels() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.fromIterable(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;

                    @Override
                    public boolean hasNext() {
                        if (++count == 2) {
                            to.dispose();
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        })
        .subscribe(to);

        to.assertValue(1)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void fusionRejected() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ASYNC);

        Observable.fromIterable(Arrays.asList(1, 2, 3))
        .subscribe(to);

        to.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3);
    }

    @Test
    public void fusionClear() {
        Observable.fromIterable(Arrays.asList(1, 2, 3))
        .subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                @SuppressWarnings("unchecked")
                QueueDisposable<Integer> qd = (QueueDisposable<Integer>)d;

                qd.requestFusion(QueueFuseable.ANY);

                try {
                    assertEquals(1, qd.poll().intValue());
                } catch (Throwable ex) {
                    fail(ex.toString());
                }

                qd.clear();
                try {
                    assertNull(qd.poll());
                } catch (Throwable ex) {
                    fail(ex.toString());
                }
            }

            @Override
            public void onNext(Integer value) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void disposeAfterHasNext() {
        TestObserver<Integer> to = new TestObserver<>();

        Observable.fromIterable(() -> new Iterator<Integer>() {
            int count;
            @Override
            public boolean hasNext() {
                if (count++ == 2) {
                    to.dispose();
                    return false;
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }
        })
        .subscribeWith(to)
        .assertValuesOnly(1, 1);
    }
}

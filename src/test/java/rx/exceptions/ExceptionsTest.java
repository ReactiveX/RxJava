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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.subjects.PublishSubject;

public class ExceptionsTest {

    @Test(expected = OnErrorNotImplementedException.class)
    public void testOnErrorNotImplementedIsThrown() {
        Observable.just(1, 2, 3).subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer t1) {
                throw new RuntimeException("hello");
            }

        });
    }

    /**
     * https://github.com/ReactiveX/RxJava/issues/3885
     */
    @Test(expected = OnCompletedFailedException.class)
    public void testOnCompletedExceptionIsThrown() {
        Observable.empty()
            .subscribe(new Subscriber<Object>() {
                @Override
                public void onCompleted() {
                    throw new RuntimeException();
                }

                @Override
                public void onError(Throwable e) {
                }

                @Override
                public void onNext(Object o) {
                }
            });
    }

    @Test
    public void testStackOverflowWouldOccur() {
        final PublishSubject<Integer> a = PublishSubject.create();
        final PublishSubject<Integer> b = PublishSubject.create();
        final int MAX_STACK_DEPTH = 800;
        final AtomicInteger depth = new AtomicInteger();
        
        a.subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(Integer n) {
                b.onNext(n + 1);
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
            public void onNext(Integer n) {
                if (depth.get() < MAX_STACK_DEPTH) { 
                    depth.set(Thread.currentThread().getStackTrace().length);
                    a.onNext(n + 1);
                }
            }
        });
        a.onNext(1);
        assertTrue(depth.get() > MAX_STACK_DEPTH);
    }
    
    @Test(expected = StackOverflowError.class)
    public void testStackOverflowErrorIsThrown() {
        Observable.just(1).subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(Integer t) {
                throw new StackOverflowError();
            }

        });
    }

    @Test(expected = ThreadDeath.class)
    public void testThreadDeathIsThrown() {
        Observable.just(1).subscribe(new Observer<Integer>() {

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
     * https://github.com/ReactiveX/RxJava/issues/969
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
        } catch (OnErrorFailedException t) {
            CompositeException cause = (CompositeException) t.getCause();
            assertTrue(cause.getExceptions().get(0) instanceof IllegalArgumentException);
            assertTrue(cause.getExceptions().get(1) instanceof IllegalStateException);
        }
    }

    /**
     * https://github.com/ReactiveX/RxJava/issues/2998
     * @throws Exception on arbitrary errors
     */
    @Test(expected = OnErrorFailedException.class)
    public void testOnErrorExceptionIsThrownFromGroupBy() throws Exception {
        Observable
            .just(1)
            .groupBy(new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer integer) {
                    throw new RuntimeException();
                }
            })
            .subscribe(new Observer<GroupedObservable<Integer, Integer>>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException();
                }

                @Override
                public void onNext(GroupedObservable<Integer, Integer> integerIntegerGroupedObservable) {

                }
            });
    }

    /**
     * https://github.com/ReactiveX/RxJava/issues/2998
     * @throws Exception on arbitrary errors
     */
    @Test(expected = OnErrorFailedException.class)
    public void testOnErrorExceptionIsThrownFromOnNext() throws Exception {
        Observable
            .just(1)
            .doOnNext(new Action1<Integer>() {
                @Override
                public void call(Integer integer) {
                    throw new RuntimeException();
                }
            })
            .subscribe(new Observer<Integer>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException();
                }

                @Override
                public void onNext(Integer integer) {

                }
            });
    }

    @Test(expected = OnErrorFailedException.class)
    public void testOnErrorExceptionIsThrownFromSubscribe() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
                              @Override
                              public void call(Subscriber<? super Integer> s1) {
                                  Observable.create(new Observable.OnSubscribe<Integer>() {
                                      @Override
                                      public void call(Subscriber<? super Integer> s2) {
                                          throw new IllegalArgumentException("original exception");
                                      }
                                  }).subscribe(s1);
                              }
                          }
        ).subscribe(new OnErrorFailedSubscriber());
    }

    @Test(expected = OnErrorFailedException.class)
    public void testOnErrorExceptionIsThrownFromUnsafeSubscribe() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
                              @Override
                              public void call(Subscriber<? super Integer> s1) {
                                  Observable.create(new Observable.OnSubscribe<Integer>() {
                                      @Override
                                      public void call(Subscriber<? super Integer> s2) {
                                          throw new IllegalArgumentException("original exception");
                                      }
                                  }).unsafeSubscribe(s1);
                              }
                          }
        ).subscribe(new OnErrorFailedSubscriber());
    }

    @Test(expected = OnErrorFailedException.class)
    public void testOnErrorExceptionIsThrownFromSingleDoOnSuccess() throws Exception {
        Single.just(1)
                .doOnSuccess(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        throw new RuntimeException();
                    }
                })
                .subscribe(new OnErrorFailedSubscriber());
    }

    @Test(expected = OnErrorFailedException.class)
    public void testOnErrorExceptionIsThrownFromSingleSubscribe() {
        Single.create(new Single.OnSubscribe<Integer>() {
                          @Override
                          public void call(SingleSubscriber<? super Integer> s1) {
                              Single.create(new Single.OnSubscribe<Integer>() {
                                  @Override
                                  public void call(SingleSubscriber<? super Integer> s2) {
                                      throw new IllegalArgumentException("original exception");
                                  }
                              }).subscribe(s1);
                          }
                      }
        ).subscribe(new OnErrorFailedSubscriber());
    }

    @Test(expected = OnErrorFailedException.class)
    public void testOnErrorExceptionIsThrownFromSingleUnsafeSubscribe() {
        Single.create(new Single.OnSubscribe<Integer>() {
                          @Override
                          public void call(final SingleSubscriber<? super Integer> s1) {
                              Single.create(new Single.OnSubscribe<Integer>() {
                                  @Override
                                  public void call(SingleSubscriber<? super Integer> s2) {
                                      throw new IllegalArgumentException("original exception");
                                  }
                              }).unsafeSubscribe(new Subscriber<Integer>() {

                                  @Override
                                  public void onCompleted() {
                                  }

                                  @Override
                                  public void onError(Throwable e) {
                                      s1.onError(e);
                                  }

                                  @Override
                                  public void onNext(Integer v) {
                                      s1.onSuccess(v);
                                  }

                              });
                          }
                      }
        ).subscribe(new OnErrorFailedSubscriber());
    }

    private class OnErrorFailedSubscriber extends Subscriber<Integer> {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            throw new RuntimeException();
        }

        @Override
        public void onNext(Integer value) {
        }
    }
}

/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.flowable;

import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.TestHelper;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

public class FlowableGenerateTest {

    @Test
    public void statefulBiconsumer() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 10;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void accept(Object s, Emitter<Object> e) throws Exception {
                e.onNext(s);
            }
        }, new Consumer<Object>() {
            @Override
            public void accept(Object d) throws Exception {

            }
        })
                .take(5)
                .test()
                .assertResult(10, 10, 10, 10, 10);
    }

    @Test
    public void stateSupplierThrows() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void accept(Object s, Emitter<Object> e) throws Exception {
                e.onNext(s);
            }
        }, Functions.emptyConsumer())
                .test()
                .assertFailure(TestException.class);
    }

    @Test
    public void generatorThrows() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void accept(Object s, Emitter<Object> e) throws Exception {
                throw new TestException();
            }
        }, Functions.emptyConsumer())
                .test()
                .assertFailure(TestException.class);
    }

    @Test
    public void disposerThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.generate(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new BiConsumer<Object, Emitter<Object>>() {
                @Override
                public void accept(Object s, Emitter<Object> e) throws Exception {
                    e.onComplete();
                }
            }, new Consumer<Object>() {
                @Override
                public void accept(Object d) throws Exception {
                    throw new TestException();
                }
            })
                    .test()
                    .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void accept(Object s, Emitter<Object> e) throws Exception {
                e.onComplete();
            }
        }, Functions.emptyConsumer()));
    }

    @Test
    public void nullError() {
        final int[] call = {0};
        Flowable.generate(Functions.justCallable(1),
                new BiConsumer<Integer, Emitter<Object>>() {
                    @Override
                    public void accept(Integer s, Emitter<Object> e) throws Exception {
                        try {
                            e.onError(null);
                        } catch (NullPointerException ex) {
                            call[0]++;
                        }
                    }
                }, Functions.emptyConsumer())
                .test()
                .assertFailure(NullPointerException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void accept(Object s, Emitter<Object> e) throws Exception {
                e.onComplete();
            }
        }, Functions.emptyConsumer()));
    }

    @Test
    public void rebatchAndTake() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void accept(Object s, Emitter<Object> e) throws Exception {
                e.onNext(1);
            }
        }, Functions.emptyConsumer())
                .rebatchRequests(1)
                .take(5)
                .test()
                .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void backpressure() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void accept(Object s, Emitter<Object> e) throws Exception {
                e.onNext(1);
            }
        }, Functions.emptyConsumer())
                .rebatchRequests(1)
                .test(5)
                .assertSubscribed()
                .assertValues(1, 1, 1, 1, 1)
                .assertNoErrors()
                .assertNotComplete();
    }

    @Test
    public void requestRace() {
        Flowable<Object> source = Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void accept(Object s, Emitter<Object> e) throws Exception {
                e.onNext(1);
            }
        }, Functions.emptyConsumer());

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Object> ts = source.test(0L);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 500; j++) {
                        ts.request(1);
                    }
                }
            };

            TestHelper.race(r, r);

            ts.assertValueCount(1000);
        }
    }

    @Test
    public void multipleOnNext() {
        Flowable.generate(new Consumer<Emitter<Object>>() {
            @Override
            public void accept(Emitter<Object> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
            }
        })
                .test(1)
                .assertFailure(IllegalStateException.class, 1);
    }

    @Test
    public void multipleOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.generate(new Consumer<Emitter<Object>>() {
                @Override
                public void accept(Emitter<Object> e) throws Exception {
                    e.onError(new TestException("First"));
                    e.onError(new TestException("Second"));
                }
            })
                    .test(1)
                    .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void multipleOnComplete() {
        Flowable.generate(new Consumer<Emitter<Object>>() {
            @Override
            public void accept(Emitter<Object> e) throws Exception {
                e.onComplete();
                e.onComplete();
            }
        })
                .test(1)
                .assertResult();
    }

    @Test
    public void flowable_bug_generate() {
        final Random random = new Random();
        Flowable.generate(new Callable<ArrayList>() {
            @Override
            public ArrayList call() throws Exception {
                return new ArrayList();
            }
        }, new BiFunction<ArrayList, Emitter<Integer>, ArrayList>() {
            @Override
            public ArrayList apply(final ArrayList list, final Emitter<Integer> emitter) throws Exception {
                final int value = random.nextInt(100);
                list.add(value);
                // change the state value in the emitter.onNext
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Thread 1");
                        emitter.onNext(value);
                    }
                }).start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Thread 1");
                        emitter.onNext(value);
                    }
                }).start();

                // emitter.onNext(value);
                if (list.size() == 5) {
                    emitter.onComplete();
                }
                return list;
            }
        })
                .subscribeOn(Schedulers.computation())
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        return String.format("[%s] %s", Thread.currentThread().getName(), integer);
                    }
                })
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        System.out.println(
                                Thread.currentThread().getName() +
                                        ": " + o);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        System.out.println("The Error: " + throwable.getMessage());
                    }
                });

        try {
            sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

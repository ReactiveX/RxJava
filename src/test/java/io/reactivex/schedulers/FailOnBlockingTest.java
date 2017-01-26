/**
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

package io.reactivex.schedulers;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;

public class FailOnBlockingTest {

    @Test
    public void failComputationFlowableBlockingFirst() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingFirst();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationFlowableBlockingLast() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingLast();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationFlowableBlockingIterable() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingIterable().iterator().next();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationFlowableBlockingSubscribe() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingSubscribe();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationFlowableBlockingSingle() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingSingle();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationFlowableBlockingForEach() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingForEach(Functions.emptyConsumer());

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationFlowableBlockingLatest() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingLatest().iterator().hasNext();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationFlowableBlockingNext() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingNext().iterator().hasNext();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationFlowableToFuture() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).toFuture().get();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableBlockingFirst() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).blockingFirst();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableBlockingLast() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingLast();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableBlockingIterable() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).blockingIterable().iterator().next();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableBlockingSubscribe() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).blockingSubscribe();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableBlockingSingle() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).blockingSingle();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableBlockingForEach() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).blockingForEach(Functions.emptyConsumer());

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableBlockingLatest() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).blockingLatest().iterator().hasNext();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableBlockingNext() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).blockingNext().iterator().hasNext();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failComputationObservableToFuture() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.computation())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).toFuture().get();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failSingleObservableBlockingFirst() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.single())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Observable.just(1).delay(10, TimeUnit.SECONDS).blockingFirst();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failSingleSingleBlockingGet() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Single.just(1)
            .subscribeOn(Schedulers.single())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Single.just(1).delay(10, TimeUnit.SECONDS).blockingGet();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failSingleMaybeBlockingGet() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Maybe.just(1)
            .subscribeOn(Schedulers.single())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Maybe.just(1).delay(10, TimeUnit.SECONDS).blockingGet();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failSingleCompletableBlockingGet() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Completable.complete()
            .subscribeOn(Schedulers.single())
            .doOnComplete(new Action() {
                @Override
                public void run() throws Exception {
                    Completable.complete().delay(10, TimeUnit.SECONDS).blockingGet();
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failSingleCompletableBlockingAwait() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Completable.complete()
            .subscribeOn(Schedulers.single())
            .doOnComplete(new Action() {
                @Override
                public void run() throws Exception {
                    Completable.complete().delay(10, TimeUnit.SECONDS).blockingAwait();
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void dontfailIOObservableBlockingFirst() {

        try {
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Observable.just(1)
            .subscribeOn(Schedulers.io())
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {
                    return Observable.just(2).delay(100, TimeUnit.MILLISECONDS).blockingFirst();
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(2);
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void failWithCustomHandler() {
        try {
            RxJavaPlugins.setOnBeforeBlocking(new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() throws Exception {
                    return true;
                }
            });
            RxJavaPlugins.setFailOnNonBlockingScheduler(true);

            Flowable.just(1)
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {

                    Flowable.just(1).delay(10, TimeUnit.SECONDS).blockingLast();

                    return v;
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalStateException.class);

        } finally {
            RxJavaPlugins.reset();
        }

        Flowable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                return Flowable.just(2).delay(100, TimeUnit.MILLISECONDS).blockingLast();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }
}

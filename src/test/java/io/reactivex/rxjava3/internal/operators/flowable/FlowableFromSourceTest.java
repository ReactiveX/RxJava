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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.List;

import org.junit.*;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableFromSourceTest extends RxJavaTest {

    PublishAsyncEmitter source;

    PublishAsyncEmitterNoCancel sourceNoCancel;

    TestSubscriberEx<Integer> ts;

    @Before
    public void before() {
        source = new PublishAsyncEmitter();
        sourceNoCancel = new PublishAsyncEmitterNoCancel();
        ts = new TestSubscriberEx<>(0L);
    }

    @Test
    public void normalBuffered() {
        Flowable.create(source, BackpressureStrategy.BUFFER).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.request(1);

        ts.assertValue(1);

        Assert.assertEquals(0, source.requested());

        ts.request(1);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalDrop() {
        Flowable.create(source, BackpressureStrategy.DROP).subscribe(ts);

        source.onNext(1);

        ts.request(1);

        ts.assertNoValues();

        source.onNext(2);
        source.onComplete();

        ts.assertValues(2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalLatest() {
        Flowable.create(source, BackpressureStrategy.LATEST).subscribe(ts);

        source.onNext(1);

        source.onNext(2);
        source.onComplete();

        ts.assertNoValues();

        ts.request(1);

        ts.assertValues(2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalMissing() {
        Flowable.create(source, BackpressureStrategy.MISSING).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalMissingRequested() {
        Flowable.create(source, BackpressureStrategy.MISSING).subscribe(ts);
        ts.request(2);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalError() {
        Flowable.create(source, BackpressureStrategy.ERROR).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();

        Assert.assertEquals("create: could not emit value due to lack of requests", ts.errors().get(0).getMessage());
    }

    @Test
    public void errorBuffered() {
        Flowable.create(source, BackpressureStrategy.BUFFER).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void errorLatest() {
        Flowable.create(source, BackpressureStrategy.LATEST).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.assertNoValues();

        ts.request(1);

        ts.assertValues(2);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void errorMissing() {
        Flowable.create(source, BackpressureStrategy.MISSING).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.request(1);

        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedBuffer() {
        Flowable.create(source, BackpressureStrategy.BUFFER).subscribe(ts);
        ts.cancel();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedLatest() {
        Flowable.create(source, BackpressureStrategy.LATEST).subscribe(ts);
        ts.cancel();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedError() {
        Flowable.create(source, BackpressureStrategy.ERROR).subscribe(ts);
        ts.cancel();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedDrop() {
        Flowable.create(source, BackpressureStrategy.DROP).subscribe(ts);
        ts.cancel();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedMissing() {
        Flowable.create(source, BackpressureStrategy.MISSING).subscribe(ts);
        ts.cancel();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedNoCancelBuffer() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.create(sourceNoCancel, BackpressureStrategy.BUFFER).subscribe(ts);
            ts.cancel();

            sourceNoCancel.onNext(1);
            sourceNoCancel.onNext(2);
            sourceNoCancel.onError(new TestException());

            ts.request(1);

            ts.assertNoValues();
            ts.assertNoErrors();
            ts.assertNotComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void unsubscribedNoCancelLatest() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.create(sourceNoCancel, BackpressureStrategy.LATEST).subscribe(ts);
            ts.cancel();

            sourceNoCancel.onNext(1);
            sourceNoCancel.onNext(2);
            sourceNoCancel.onError(new TestException());

            ts.request(1);

            ts.assertNoValues();
            ts.assertNoErrors();
            ts.assertNotComplete();
            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void unsubscribedNoCancelError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.create(sourceNoCancel, BackpressureStrategy.ERROR).subscribe(ts);
            ts.cancel();

            sourceNoCancel.onNext(1);
            sourceNoCancel.onNext(2);
            sourceNoCancel.onError(new TestException());

            ts.request(1);

            ts.assertNoValues();
            ts.assertNoErrors();
            ts.assertNotComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void unsubscribedNoCancelDrop() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.create(sourceNoCancel, BackpressureStrategy.DROP).subscribe(ts);
            ts.cancel();

            sourceNoCancel.onNext(1);
            sourceNoCancel.onNext(2);
            sourceNoCancel.onError(new TestException());

            ts.request(1);

            ts.assertNoValues();
            ts.assertNoErrors();
            ts.assertNotComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void unsubscribedNoCancelMissing() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.create(sourceNoCancel, BackpressureStrategy.MISSING).subscribe(ts);
            ts.cancel();

            sourceNoCancel.onNext(1);
            sourceNoCancel.onNext(2);
            sourceNoCancel.onError(new TestException());

            ts.request(1);

            ts.assertNoValues();
            ts.assertNoErrors();
            ts.assertNotComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void deferredRequest() {
        Flowable.create(source, BackpressureStrategy.BUFFER).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.request(2);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void take() {
        Flowable.create(source, BackpressureStrategy.BUFFER).take(2).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.request(2);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void takeOne() {
        Flowable.create(source, BackpressureStrategy.BUFFER).take(1).subscribe(ts);
        ts.request(2);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void requestExact() {
        Flowable.create(source, BackpressureStrategy.BUFFER).subscribe(ts);
        ts.request(2);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void takeNoCancel() {
        Flowable.create(sourceNoCancel, BackpressureStrategy.BUFFER).take(2).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onComplete();

        ts.request(2);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void takeOneNoCancel() {
        Flowable.create(sourceNoCancel, BackpressureStrategy.BUFFER).take(1).subscribe(ts);
        ts.request(2);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onComplete();

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void unsubscribeNoCancel() {
        Flowable.create(sourceNoCancel, BackpressureStrategy.BUFFER).subscribe(ts);
        ts.request(2);

        sourceNoCancel.onNext(1);

        ts.cancel();

        sourceNoCancel.onNext(2);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribeInline() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
            }
        };

        Flowable.create(sourceNoCancel, BackpressureStrategy.BUFFER).subscribe(ts1);

        sourceNoCancel.onNext(1);

        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
    }

    @Test
    public void completeInline() {
        Flowable.create(sourceNoCancel, BackpressureStrategy.BUFFER).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onComplete();

        ts.request(2);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void errorInline() {
        Flowable.create(sourceNoCancel, BackpressureStrategy.BUFFER).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onError(new TestException());

        ts.request(2);

        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void requestInline() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                request(1);
            }
        };

        Flowable.create(sourceNoCancel, BackpressureStrategy.BUFFER).subscribe(ts1);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);

        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
    }

    @Test
    public void unsubscribeInlineLatest() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
            }
        };

        Flowable.create(sourceNoCancel, BackpressureStrategy.LATEST).subscribe(ts1);

        sourceNoCancel.onNext(1);

        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
    }

    @Test
    public void unsubscribeInlineExactLatest() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
            }
        };

        Flowable.create(sourceNoCancel, BackpressureStrategy.LATEST).subscribe(ts1);

        sourceNoCancel.onNext(1);

        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
    }

    @Test
    public void completeInlineLatest() {
        Flowable.create(sourceNoCancel, BackpressureStrategy.LATEST).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onComplete();

        ts.request(2);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void completeInlineExactLatest() {
        Flowable.create(sourceNoCancel, BackpressureStrategy.LATEST).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onComplete();

        ts.request(1);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void errorInlineLatest() {
        Flowable.create(sourceNoCancel, BackpressureStrategy.LATEST).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onError(new TestException());

        ts.request(2);

        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void requestInlineLatest() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                request(1);
            }
        };

        Flowable.create(sourceNoCancel, BackpressureStrategy.LATEST).subscribe(ts1);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);

        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
    }

    static final class PublishAsyncEmitter implements FlowableOnSubscribe<Integer>, FlowableSubscriber<Integer> {

        final PublishProcessor<Integer> processor;

        FlowableEmitter<Integer> current;

        PublishAsyncEmitter() {
            this.processor = PublishProcessor.create();
        }

        long requested() {
            return current.requested();
        }

        @Override
        public void subscribe(final FlowableEmitter<Integer> t) {

            this.current = t;

            final ResourceSubscriber<Integer> as = new ResourceSubscriber<Integer>() {

                @Override
                public void onComplete() {
                    t.onComplete();
                }

                @Override
                public void onError(Throwable e) {
                    t.onError(e);
                }

                @Override
                public void onNext(Integer v) {
                    t.onNext(v);
                }

            };

            processor.subscribe(as);

            t.setCancellable(new Cancellable() {
                @Override
                public void cancel() throws Exception {
                    as.dispose();
                }
            });;
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Integer t) {
            processor.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            processor.onError(e);
        }

        @Override
        public void onComplete() {
            processor.onComplete();
        }
    }

    static final class PublishAsyncEmitterNoCancel implements FlowableOnSubscribe<Integer>, FlowableSubscriber<Integer> {

        final PublishProcessor<Integer> processor;

        PublishAsyncEmitterNoCancel() {
            this.processor = PublishProcessor.create();
        }

        @Override
        public void subscribe(final FlowableEmitter<Integer> t) {

            processor.subscribe(new FlowableSubscriber<Integer>() {

                @Override
                public void onSubscribe(Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onComplete() {
                    t.onComplete();
                }

                @Override
                public void onError(Throwable e) {
                    t.onError(e);
                }

                @Override
                public void onNext(Integer v) {
                    t.onNext(v);
                }

            });
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Integer t) {
            processor.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            processor.onError(e);
        }

        @Override
        public void onComplete() {
            processor.onComplete();
        }
    }

}

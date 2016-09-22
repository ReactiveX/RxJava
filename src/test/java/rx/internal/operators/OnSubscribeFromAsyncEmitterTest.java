/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.*;

import rx.*;
import rx.exceptions.*;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaHooks;
import rx.subjects.PublishSubject;

/**
 * @deprecated because AsyncEmitter is deprecated (replaced by Emitter).
 */
@Deprecated
public class OnSubscribeFromAsyncEmitterTest {

    PublishAsyncEmitter source;

    PublishAsyncEmitterNoCancel sourceNoCancel;

    TestSubscriber<Integer> ts;

    @Before
    public void before() {
        source = new PublishAsyncEmitter();
        sourceNoCancel = new PublishAsyncEmitterNoCancel();
        ts = TestSubscriber.create(0L);
    }

    @Test
    public void normalBuffered() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.requestMore(1);

        ts.assertValue(1);

        Assert.assertEquals(0, source.requested());

        ts.requestMore(1);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void normalDrop() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);

        source.onNext(1);

        ts.requestMore(1);

        source.onNext(2);
        source.onCompleted();

        ts.assertValues(2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void normalLatest() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);

        source.onNext(1);

        source.onNext(2);
        source.onCompleted();

        ts.requestMore(1);

        ts.assertValues(2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void normalNone() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void normalNoneRequested() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
        ts.requestMore(2);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }


    @Test
    public void normalError() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotCompleted();

        Assert.assertEquals("fromEmitter: could not emit value due to lack of requests", ts.getOnErrorEvents().get(0).getMessage());
    }

    @Test
    public void overflowErrorIsNotFollowedByAnotherErrorDueToOnNextFromUpstream() {
        Action1<AsyncEmitter<Integer>> source = new Action1<AsyncEmitter<Integer>>(){

            @Override
            public void call(AsyncEmitter<Integer> emitter) {
                emitter.onNext(1);
                //don't check for unsubscription
                emitter.onNext(2);
            }};
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.ERROR).unsafeSubscribe(ts);

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotCompleted();

        Assert.assertEquals("fromEmitter: could not emit value due to lack of requests", ts.getOnErrorEvents().get(0).getMessage());
    }

    @Test
    public void overflowErrorIsNotFollowedByAnotherCompletedDueToCompletedFromUpstream() {
        Action1<AsyncEmitter<Integer>> source = new Action1<AsyncEmitter<Integer>>(){

            @Override
            public void call(AsyncEmitter<Integer> emitter) {
                emitter.onNext(1);
                //don't check for unsubscription
                emitter.onCompleted();
            }};
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.ERROR).unsafeSubscribe(ts);

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotCompleted();

        Assert.assertEquals("fromEmitter: could not emit value due to lack of requests", ts.getOnErrorEvents().get(0).getMessage());
    }

    @Test
    public void overflowErrorIsNotFollowedByAnotherErrorDueToOnErrorFromUpstreamAndSecondErrorIsReportedToHook() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
            RxJavaHooks.setOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    list.add(t);
                }});
            final RuntimeException e = new RuntimeException();
            Action1<AsyncEmitter<Integer>> source = new Action1<AsyncEmitter<Integer>>(){

                @Override
                public void call(AsyncEmitter<Integer> emitter) {
                    emitter.onNext(1);
                    //don't check for unsubscription
                    emitter.onError(e);
                }};
            Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.ERROR).unsafeSubscribe(ts);

            ts.assertNoValues();
            ts.assertError(MissingBackpressureException.class);
            ts.assertNotCompleted();

            Assert.assertEquals("fromEmitter: could not emit value due to lack of requests", ts.getOnErrorEvents().get(0).getMessage());
            assertEquals(Arrays.asList(e), list);
        } finally {
            RxJavaHooks.reset();
        }
    }

    @Test
    public void errorBuffered() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.requestMore(1);

        ts.assertValue(1);

        ts.requestMore(1);

        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void errorLatest() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.requestMore(1);

        ts.assertValues(2);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void errorNone() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.requestMore(1);

        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedBuffer() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        ts.unsubscribe();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedLatest() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        ts.unsubscribe();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedError() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);
        ts.unsubscribe();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedDrop() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);
        ts.unsubscribe();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedNone() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
        ts.unsubscribe();

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedNoCancelBuffer() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        ts.unsubscribe();

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedNoCancelLatest() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        ts.unsubscribe();

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedNoCancelError() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);
        ts.unsubscribe();

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedNoCancelDrop() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);
        ts.unsubscribe();

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribedNoCancelNone() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
        ts.unsubscribe();

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void deferredRequest() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.requestMore(2);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void take() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.BUFFER).take(2).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.requestMore(2);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void takeOne() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.BUFFER).take(1).subscribe(ts);
        ts.requestMore(2);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void requestExact() {
        Observable.fromEmitter(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        ts.requestMore(2);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void takeNoCancel() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).take(2).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onCompleted();

        ts.requestMore(2);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void takeOneNoCancel() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).take(1).subscribe(ts);
        ts.requestMore(2);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onCompleted();

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void unsubscribeNoCancel() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        ts.requestMore(2);

        sourceNoCancel.onNext(1);

        ts.unsubscribe();

        sourceNoCancel.onNext(2);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }


    @Test
    public void unsubscribeInline() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                unsubscribe();
            }
        };

        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts1);

        sourceNoCancel.onNext(1);

        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotCompleted();
    }

    @Test
    public void completeInline() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onCompleted();

        ts.requestMore(2);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void errorInline() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onError(new TestException());

        ts.requestMore(2);

        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void requestInline() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                requestMore(1);
            }
        };

        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts1);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);

        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.assertNotCompleted();
    }

    @Test
    public void unsubscribeInlineLatest() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                unsubscribe();
            }
        };

        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);

        sourceNoCancel.onNext(1);

        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotCompleted();
    }

    @Test
    public void unsubscribeInlineExactLatest() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                unsubscribe();
            }
        };

        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);

        sourceNoCancel.onNext(1);

        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotCompleted();
    }

    @Test
    public void completeInlineLatest() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onCompleted();

        ts.requestMore(2);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void completeInlineExactLatest() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onCompleted();

        ts.requestMore(1);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void errorInlineLatest() {
        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onError(new TestException());

        ts.requestMore(2);

        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void requestInlineLatest() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                requestMore(1);
            }
        };

        Observable.fromEmitter(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);

        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);

        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.assertNotCompleted();
    }

    static final class PublishAsyncEmitter implements Action1<AsyncEmitter<Integer>>, Observer<Integer> {

        final PublishSubject<Integer> subject;

        AsyncEmitter<Integer> current;

        public PublishAsyncEmitter() {
            this.subject = PublishSubject.create();
        }

        long requested() {
            return current.requested();
        }

        @Override
        public void call(final AsyncEmitter<Integer> t) {

            this.current = t;

            final Subscription s = subject.subscribe(new Observer<Integer>() {

                @Override
                public void onCompleted() {
                    t.onCompleted();
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

            t.setCancellation(new AsyncEmitter.Cancellable() {
                @Override
                public void cancel() throws Exception {
                    s.unsubscribe();
                }
            });;
        }

        @Override
        public void onNext(Integer t) {
            subject.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            subject.onError(e);
        }

        @Override
        public void onCompleted() {
            subject.onCompleted();
        }
    }

    static final class PublishAsyncEmitterNoCancel implements Action1<AsyncEmitter<Integer>>, Observer<Integer> {

        final PublishSubject<Integer> subject;

        public PublishAsyncEmitterNoCancel() {
            this.subject = PublishSubject.create();
        }

        @Override
        public void call(final AsyncEmitter<Integer> t) {

            subject.subscribe(new Observer<Integer>() {

                @Override
                public void onCompleted() {
                    t.onCompleted();
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
        public void onNext(Integer t) {
            subject.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            subject.onError(e);
        }

        @Override
        public void onCompleted() {
            subject.onCompleted();
        }
    }

}

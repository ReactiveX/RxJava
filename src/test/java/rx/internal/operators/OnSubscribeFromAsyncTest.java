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

import org.junit.*;

import rx.*;
import rx.exceptions.*;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OnSubscribeFromAsyncTest {

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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);
        
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
        
        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void normalNoneRequested() {
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);
        
        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotCompleted();
        
        Assert.assertEquals("fromAsync: could not emit value due to lack of requests", ts.getOnErrorEvents().get(0).getMessage());
    }

    @Test
    public void errorBuffered() {
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
        
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
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
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
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
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
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
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);
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
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);
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
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).take(2).subscribe(ts);
        
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).take(1).subscribe(ts);
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
        Observable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
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
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).take(2).subscribe(ts);
        
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
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).take(1).subscribe(ts);
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
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
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
        
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts1);
        
        sourceNoCancel.onNext(1);
        
        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotCompleted();
    }

    @Test
    public void completeInline() {
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onCompleted();
        
        ts.requestMore(2);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void errorInline() {
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
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
        
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts1);
        
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
        
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);
        
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
        
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);
        
        sourceNoCancel.onNext(1);
        
        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotCompleted();
    }

    @Test
    public void completeInlineLatest() {
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onCompleted();
        
        ts.requestMore(2);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void completeInlineExactLatest() {
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onCompleted();
        
        ts.requestMore(1);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void errorInlineLatest() {
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
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
        
        Observable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);
        
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

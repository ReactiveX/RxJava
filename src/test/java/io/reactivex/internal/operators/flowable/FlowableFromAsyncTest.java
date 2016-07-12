package io.reactivex.internal.operators.flowable;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.*;

public class FlowableFromAsyncTest {

    PublishAsyncEmitter source;

    PublishAsyncEmitterNoCancel sourceNoCancel;

    TestSubscriber<Integer> ts;
    
    @Before
    public void before() {
        source = new PublishAsyncEmitter();
        sourceNoCancel = new PublishAsyncEmitterNoCancel();
        ts = new TestSubscriber<Integer>(0L);
    }
    
    @Test
    public void normalBuffered() {
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);
        
        source.onNext(1);

        ts.request(1);

        source.onNext(2);
        source.onComplete();
        
        ts.assertValues(2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalLatest() {
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
        source.onNext(1);

        source.onNext(2);
        source.onComplete();

        ts.request(1);

        ts.assertValues(2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalNone() {
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
        
        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalNoneRequested() {
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);
        
        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();
        
        Assert.assertEquals("fromAsync: could not emit value due to lack of requests", ts.errors().get(0).getMessage());
    }

    @Test
    public void errorBuffered() {
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());
        
        ts.request(1);
        
        ts.assertValues(2);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }
    
    @Test
    public void errorNone() {
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
        
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);
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
    public void unsubscribedNone() {
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
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
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        ts.cancel();
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());
        
        ts.request(1);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedNoCancelLatest() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        ts.cancel();
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());
        
        ts.request(1);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedNoCancelError() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.ERROR).subscribe(ts);
        ts.cancel();
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());
        
        ts.request(1);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedNoCancelDrop() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.DROP).subscribe(ts);
        ts.cancel();
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());
        
        ts.request(1);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribedNoCancelNone() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.NONE).subscribe(ts);
        ts.cancel();
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        sourceNoCancel.onError(new TestException());
        
        ts.request(1);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void deferredRequest() {
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).take(2).subscribe(ts);
        
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).take(1).subscribe(ts);
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
        Flowable.fromAsync(source, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
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
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).take(2).subscribe(ts);
        
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
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).take(1).subscribe(ts);
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
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
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
        
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts1);
        
        sourceNoCancel.onNext(1);
        
        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
    }

    @Test
    public void completeInline() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onComplete();
        
        ts.request(2);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void errorInline() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts);
        
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
        
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.BUFFER).subscribe(ts1);
        
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
        
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);
        
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
        
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);
        
        sourceNoCancel.onNext(1);
        
        ts1.assertValues(1);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
    }

    @Test
    public void completeInlineLatest() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onComplete();
        
        ts.request(2);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void completeInlineExactLatest() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onComplete();
        
        ts.request(1);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void errorInlineLatest() {
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts);
        
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
        
        Flowable.fromAsync(sourceNoCancel, AsyncEmitter.BackpressureMode.LATEST).subscribe(ts1);
        
        sourceNoCancel.onNext(1);
        sourceNoCancel.onNext(2);
        
        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
    }
    
    static final class PublishAsyncEmitter implements Consumer<AsyncEmitter<Integer>>, Subscriber<Integer> {
        
        final PublishProcessor<Integer> subject;
        
        AsyncEmitter<Integer> current;
        
        public PublishAsyncEmitter() {
            this.subject = PublishProcessor.create();
        }
        
        long requested() {
            return current.requested();
        }
        
        @Override
        public void accept(final AsyncEmitter<Integer> t) {

            this.current = t;
            
            final AsyncSubscriber<Integer> as = new AsyncSubscriber<Integer>() {

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
            
            subject.subscribe(as);
            
            t.setCancellation(new AsyncEmitter.Cancellable() {
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
            subject.onNext(t);
        }
        
        @Override
        public void onError(Throwable e) {
            subject.onError(e);
        }
        
        @Override
        public void onComplete() {
            subject.onComplete();
        }
    }
    
    static final class PublishAsyncEmitterNoCancel implements Consumer<AsyncEmitter<Integer>>, Subscriber<Integer> {
        
        final PublishProcessor<Integer> subject;
        
        public PublishAsyncEmitterNoCancel() {
            this.subject = PublishProcessor.create();
        }
        
        @Override
        public void accept(final AsyncEmitter<Integer> t) {

            subject.subscribe(new Subscriber<Integer>() {

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
            subject.onNext(t);
        }
        
        @Override
        public void onError(Throwable e) {
            subject.onError(e);
        }
        
        @Override
        public void onComplete() {
            subject.onComplete();
        }
    }

}
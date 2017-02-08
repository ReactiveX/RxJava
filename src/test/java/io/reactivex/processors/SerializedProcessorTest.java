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

package io.reactivex.processors;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class SerializedProcessorTest {

    @Test
    public void testBasic() {
        SerializedProcessor<String> subject = new SerializedProcessor<String>(PublishProcessor.<String> create());
        TestSubscriber<String> ts = new TestSubscriber<String>();
        subject.subscribe(ts);
        subject.onNext("hello");
        subject.onComplete();
        ts.awaitTerminalEvent();
        ts.assertValue("hello");
    }

    @Test
    public void testAsyncSubjectValueRelay() {
        AsyncProcessor<Integer> async = AsyncProcessor.create();
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testAsyncSubjectValueEmpty() {
        AsyncProcessor<Integer> async = AsyncProcessor.create();
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testAsyncSubjectValueError() {
        AsyncProcessor<Integer> async = AsyncProcessor.create();
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testPublishSubjectValueRelay() {
        PublishProcessor<Integer> async = PublishProcessor.create();
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
    }

    @Test
    public void testPublishSubjectValueEmpty() {
        PublishProcessor<Integer> async = PublishProcessor.create();
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
    }
    @Test
    public void testPublishSubjectValueError() {
        PublishProcessor<Integer> async = PublishProcessor.create();
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
    }

    @Test
    public void testBehaviorSubjectValueRelay() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectValueRelayIncomplete() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        async.onNext(1);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectIncompleteEmpty() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectEmpty() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectError() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testReplaySubjectValueRelay() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayIncomplete() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        async.onNext(1);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBounded() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBoundedIncomplete() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBoundedEmptyIncomplete() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayEmptyIncomplete() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testReplaySubjectEmpty() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectError() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testReplaySubjectBoundedEmpty() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectBoundedError() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testDontWrapSerializedSubjectAgain() {
        PublishProcessor<Object> s = PublishProcessor.create();
        FlowableProcessor<Object> s1 = s.toSerialized();
        FlowableProcessor<Object> s2 = s1.toSerialized();
        assertSame(s1, s2);
    }

    @Test
    public void normal() {
        FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

        TestSubscriber<Integer> ts = s.test();

        Flowable.range(1, 10).subscribe(s);

        ts.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertFalse(s.hasSubscribers());

        s.onNext(11);

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            s.onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        s.onComplete();

        BooleanSubscription bs = new BooleanSubscription();
        s.onSubscribe(bs);
        assertTrue(bs.isCancelled());
    }

    @Test
    public void onNextOnNextRace() {
        for (int i = 0; i < 500; i++) {
            final FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

            TestSubscriber<Integer> ts = s.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    s.onNext(2);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertSubscribed().assertNoErrors().assertNotComplete()
            .assertValueSet(Arrays.asList(1, 2));
        }
    }

    @Test
    public void onNextOnErrorRace() {
        for (int i = 0; i < 500; i++) {
            final FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

            TestSubscriber<Integer> ts = s.test();

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    s.onError(ex);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertError(ex).assertNotComplete();

            if (ts.valueCount() != 0) {
                ts.assertValue(1);
            }
        }
    }

    @Test
    public void onNextOnCompleteRace() {
        for (int i = 0; i < 500; i++) {
            final FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

            TestSubscriber<Integer> ts = s.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    s.onComplete();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertComplete().assertNoErrors();

            if (ts.valueCount() != 0) {
                ts.assertValue(1);
            }
        }
    }

    @Test
    public void onNextOnSubscribeRace() {
        for (int i = 0; i < 500; i++) {
            final FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

            TestSubscriber<Integer> ts = s.test();

            final BooleanSubscription bs = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    s.onSubscribe(bs);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertValue(1).assertNotComplete().assertNoErrors();
        }
    }

    @Test
    public void onCompleteOnSubscribeRace() {
        for (int i = 0; i < 500; i++) {
            final FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

            TestSubscriber<Integer> ts = s.test();

            final BooleanSubscription bs = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    s.onSubscribe(bs);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertResult();
        }
    }

    @Test
    public void onCompleteOnCompleteRace() {
        for (int i = 0; i < 500; i++) {
            final FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

            TestSubscriber<Integer> ts = s.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    s.onComplete();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertResult();
        }
    }

    @Test
    public void onErrorOnErrorRace() {
        for (int i = 0; i < 500; i++) {
            final FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

            TestSubscriber<Integer> ts = s.test();

            final TestException ex = new TestException();

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        s.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        s.onError(ex);
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());

                ts.assertFailure(TestException.class);

                TestHelper.assertUndeliverable(errors, 0, TestException.class);
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void onSubscribeOnSubscribeRace() {
        for (int i = 0; i < 500; i++) {
            final FlowableProcessor<Integer> s = PublishProcessor.<Integer>create().toSerialized();

            TestSubscriber<Integer> ts = s.test();

            final BooleanSubscription bs1 = new BooleanSubscription();
            final BooleanSubscription bs2 = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.onSubscribe(bs1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    s.onSubscribe(bs2);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertEmpty();
        }
    }
}

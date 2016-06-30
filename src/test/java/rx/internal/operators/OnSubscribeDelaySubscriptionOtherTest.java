/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import java.util.concurrent.atomic.*;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OnSubscribeDelaySubscriptionOtherTest {
    @Test
    public void testNoPrematureSubscription() {
        PublishSubject<Object> other = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        final AtomicInteger subscribed = new AtomicInteger();
        
        Observable.just(1)
        .doOnSubscribe(new Action0() {
            @Override
            public void call() {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();
        
        Assert.assertEquals("Premature subscription", 0, subscribed.get());
        
        other.onNext(1);
        
        Assert.assertEquals("No subscription", 1, subscribed.get());
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void testNoMultipleSubscriptions() {
        PublishSubject<Object> other = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        final AtomicInteger subscribed = new AtomicInteger();
        
        Observable.just(1)
        .doOnSubscribe(new Action0() {
            @Override
            public void call() {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();
        
        Assert.assertEquals("Premature subscription", 0, subscribed.get());
        
        other.onNext(1);
        other.onNext(2);
        
        Assert.assertEquals("No subscription", 1, subscribed.get());
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void testCompleteTriggersSubscription() {
        PublishSubject<Object> other = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        final AtomicInteger subscribed = new AtomicInteger();
        
        Observable.just(1)
        .doOnSubscribe(new Action0() {
            @Override
            public void call() {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();
        
        Assert.assertEquals("Premature subscription", 0, subscribed.get());
        
        other.onCompleted();
        
        Assert.assertEquals("No subscription", 1, subscribed.get());
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void testNoPrematureSubscriptionToError() {
        PublishSubject<Object> other = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        final AtomicInteger subscribed = new AtomicInteger();
        
        Observable.<Integer>error(new TestException())
        .doOnSubscribe(new Action0() {
            @Override
            public void call() {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();
        
        Assert.assertEquals("Premature subscription", 0, subscribed.get());
        
        other.onCompleted();
        
        Assert.assertEquals("No subscription", 1, subscribed.get());
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
    
    @Test
    public void testNoSubscriptionIfOtherErrors() {
        PublishSubject<Object> other = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        final AtomicInteger subscribed = new AtomicInteger();
        
        Observable.<Integer>error(new TestException())
        .doOnSubscribe(new Action0() {
            @Override
            public void call() {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();
        
        Assert.assertEquals("Premature subscription", 0, subscribed.get());
        
        other.onError(new TestException());
        
        Assert.assertEquals("Premature subscription", 0, subscribed.get());
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
    
    @Test
    public void testBackpressurePassesThrough() {
        
        PublishSubject<Object> other = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        
        final AtomicInteger subscribed = new AtomicInteger();
        
        Observable.just(1, 2, 3, 4, 5)
        .doOnSubscribe(new Action0() {
            @Override
            public void call() {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();
        
        Assert.assertEquals("Premature subscription", 0, subscribed.get());
        
        other.onNext(1);
        
        Assert.assertEquals("No subscription", 1, subscribed.get());

        Assert.assertFalse("Not unsubscribed from other", other.hasObservers());
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertNoValues();
        
        ts.requestMore(1);
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(2);
        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(10);
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void unsubscriptionPropagatesBeforeSubscribe() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.delaySubscription(other).subscribe(ts);
        
        Assert.assertFalse("source subscribed?", source.hasObservers());
        Assert.assertTrue("other not subscribed?", other.hasObservers());
        
        ts.unsubscribe();
        
        Assert.assertFalse("source subscribed?", source.hasObservers());
        Assert.assertFalse("other still subscribed?", other.hasObservers());
    }

    @Test
    public void unsubscriptionPropagatesAfterSubscribe() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.delaySubscription(other).subscribe(ts);
        
        Assert.assertFalse("source subscribed?", source.hasObservers());
        Assert.assertTrue("other not subscribed?", other.hasObservers());
        
        other.onCompleted();
        
        Assert.assertTrue("source not subscribed?", source.hasObservers());
        Assert.assertFalse("other still subscribed?", other.hasObservers());
        
        ts.unsubscribe();
        
        Assert.assertFalse("source subscribed?", source.hasObservers());
        Assert.assertFalse("other still subscribed?", other.hasObservers());
    }

    @Test
    public void delayAndTakeUntilNeverSubscribeToSource() {
        PublishSubject<Integer> delayUntil = PublishSubject.create();
        PublishSubject<Integer> interrupt = PublishSubject.create();
        final AtomicBoolean subscribed = new AtomicBoolean(false);

        Observable.just(1)
        .doOnSubscribe(new Action0() {
            @Override
            public void call() {
                subscribed.set(true);
            }
        })
        .delaySubscription(delayUntil)
        .takeUntil(interrupt)
        .subscribe();

        interrupt.onNext(9000);
        delayUntil.onNext(1);

        Assert.assertFalse(subscribed.get());
    }

}

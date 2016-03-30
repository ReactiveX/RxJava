package rx.internal.operators;

import org.junit.Assert;
import org.junit.Test;
import rx.Single;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleOnSubscribeDelaySubscriptionOtherTest {
    @Test
    public void noPrematureSubscription() {
        PublishSubject<Object> other = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger subscribed = new AtomicInteger();

        Single.just(1)
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
    public void noPrematureSubscriptionToError() {
        PublishSubject<Object> other = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger subscribed = new AtomicInteger();

        Single.<Integer>error(new TestException())
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

        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }

    @Test
    public void noSubscriptionIfOtherErrors() {
        PublishSubject<Object> other = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger subscribed = new AtomicInteger();

        Single.<Integer>error(new TestException())
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
}

package io.reactivex.internal.operators;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.function.BiPredicate;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;

public class OperatorSequenceEqualTest {

    @Test
    public void test1() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.just("one", "two", "three"));
        verifyResult(observable, true);
    }

    @Test
    public void test2() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.just("one", "two", "three", "four"));
        verifyResult(observable, false);
    }

    @Test
    public void test3() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.just("one", "two", "three", "four"),
                Observable.just("one", "two", "three"));
        verifyResult(observable, false);
    }

    @Test
    public void testWithError1() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())),
                Observable.just("one", "two", "three"));
        verifyError(observable);
    }

    @Test
    public void testWithError2() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())));
        verifyError(observable);
    }

    @Test
    public void testWithError3() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())),
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())));
        verifyError(observable);
    }

    @Test
    public void testWithEmpty1() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.<String> empty(),
                Observable.just("one", "two", "three"));
        verifyResult(observable, false);
    }

    @Test
    public void testWithEmpty2() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.<String> empty());
        verifyResult(observable, false);
    }

    @Test
    public void testWithEmpty3() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.<String> empty(), Observable.<String> empty());
        verifyResult(observable, true);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull1() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.just((String) null), Observable.just("one"));
        verifyResult(observable, false);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull2() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.just((String) null), Observable.just((String) null));
        verifyResult(observable, true);
    }

    @Test
    public void testWithEqualityError() {
        Observable<Boolean> observable = Observable.sequenceEqual(
                Observable.just("one"), Observable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                });
        verifyError(observable);
    }

    private void verifyResult(Observable<Boolean> observable, boolean result) {
        Subscriber<Boolean> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(result);
        inOrder.verify(observer).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Observable<Boolean> observable) {
        Subscriber<Boolean> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }
}
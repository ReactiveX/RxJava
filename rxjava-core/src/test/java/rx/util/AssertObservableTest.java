package rx.util;

import org.junit.Test;

import rx.Observable;

public class AssertObservableTest {

    @Test
    public void testPassNotNull() {
        AssertObservable.assertObservableEqualsBlocking("foo", Observable.from(1, 2), Observable.from(1, 2));
    }

    @Test
    public void testPassNull() {
        AssertObservable.assertObservableEqualsBlocking("foo", null, null);
    }

    @Test(expected = AssertionError.class)
    public void testFailNotNull() {
        AssertObservable.assertObservableEqualsBlocking("foo", Observable.from(1, 2), Observable.from(1));
    }

    @Test(expected = AssertionError.class)
    public void testFailNull() {
        AssertObservable.assertObservableEqualsBlocking("foo", Observable.from(1, 2), null);
    }
}

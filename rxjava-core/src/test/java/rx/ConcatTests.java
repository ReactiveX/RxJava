package rx;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ConcatTests {

    @Test
    public void testConcatSimple() {
        Observable<String> o1 = Observable.from("one", "two");
        Observable<String> o2 = Observable.from("three", "four");

        List<String> values = Observable.concat(o1, o2).toList().toBlockingObservable().single();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
    }

    @Test
    public void testConcatWithObservableOfObservable() {
        Observable<String> o1 = Observable.from("one", "two");
        Observable<String> o2 = Observable.from("three", "four");
        Observable<String> o3 = Observable.from("five", "six");

        Observable<Observable<String>> os = Observable.from(o1, o2, o3);

        List<String> values = Observable.concat(os).toList().toBlockingObservable().single();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
    }

    @Test
    public void testConcatWithIterableOfObservable() {
        Observable<String> o1 = Observable.from("one", "two");
        Observable<String> o2 = Observable.from("three", "four");
        Observable<String> o3 = Observable.from("five", "six");

        @SuppressWarnings("unchecked")
        Iterable<Observable<String>> is = Arrays.asList(o1, o2, o3);

        List<String> values = Observable.concat(Observable.from(is)).toList().toBlockingObservable().single();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
    }
}

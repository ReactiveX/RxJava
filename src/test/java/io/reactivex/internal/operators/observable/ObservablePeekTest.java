package io.reactivex.internal.operators.observable;

import io.reactivex.Observable;
import io.reactivex.functions.ImmutableConsumer;
import io.reactivex.observers.TestObserver;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ObservablePeekTest {

    @Test
    public void testOnPeekEmitsForEachElement() throws Exception {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        ImmutableConsumer<String> testConsumer = (ImmutableConsumer<String>) mock(ImmutableConsumer.class);

        w.peek(testConsumer).subscribe();

        verify(testConsumer, times(1)).accept("one");
        verify(testConsumer, times(1)).accept("two");
        verify(testConsumer, times(1)).accept("three");
    }

    @Test
    public void testOnPeekEmitsNothingForEmpty() throws Exception {
        Observable<String> w = Observable.empty();
        ImmutableConsumer<String> testConsumer = (ImmutableConsumer<String>) mock(ImmutableConsumer.class);

        w.peek(testConsumer).subscribe();

        verify(testConsumer, never()).accept("");
    }

    @Test
    @Ignore("Null values are not allowed")
    public void testListWithNullValue() throws Exception {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", null, "three"));
        ImmutableConsumer<String> testConsumer = (ImmutableConsumer<String>) mock(ImmutableConsumer.class);

        TestObserver<String> testObserver = TestObserver.create();

        w.peek(testConsumer).subscribe(testObserver);

        verify(testConsumer, times(1)).accept("one");
        verify(testConsumer, times(1)).accept(null);
        verify(testConsumer, times(1)).accept("three");
    }

    
}

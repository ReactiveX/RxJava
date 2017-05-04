/*
 * This is the confidential unpublished intellectual property of EMC Corporation,
 * and includes without limitation exclusive copyright and trade secret rights
 * of EMC throughout the world.
 */
package rx.internal.operators;
;
import org.junit.Test;
import rx.Observer;
import rx.Single;

import static org.mockito.Mockito.*;

public class SingleOperatorCastTest {

    @Test
    public void testSingleCast() {
        Single<?> source = Single.just(1);
        Single<Integer> single = source.cast(Integer.class);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        single.subscribe(observer);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(1);
        verify(observer, never()).onError(
            org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSingleCastWithWrongType() {
        Single<?> source = Single.just(1);
        Single<Boolean> single = source.cast(Boolean.class);

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        single.subscribe(observer);
        verify(observer, times(1)).onError(
            org.mockito.Matchers.any(ClassCastException.class));
    }
}

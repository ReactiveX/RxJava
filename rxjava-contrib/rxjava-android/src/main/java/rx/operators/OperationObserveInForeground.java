package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;

public class OperationObserveInForeground {

    public static <T> Observable<T> observeInForeground(Observable<T> source) {
        return Observable.create(new ObserveInForeground<T>(source));
    }

    private static final class ObserveInForeground<T> implements Observable.OnSubscribeFunc<T> {

        private final Observable<T> source;

        private ObserveInForeground(Observable<T> source) {
            this.source = source;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            if (!isValidObserver(observer)) {
                throw new IllegalArgumentException("Observer must be one of: " +
                 FragmentObserver.class.getCanonicalName() + "\n" +
                 SupportFragmentObserver.class.getCanonicalName());
            }
            return source.observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
        }

        private boolean isValidObserver(final Observer<? super T> observer) {
            return observer instanceof FragmentObserver || observer instanceof SupportFragmentObserver;
        }
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE)
    public static final class UnitTest {

        @Mock
        private FragmentObserver mockObserver;

        @Mock
        private Observable<Integer> mockObservable;

        @Before
        public void setupMocks() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void itObservesTheSourceSequenceOnTheMainUIThread() {
            OperationObserveInForeground.observeInForeground(mockObservable).subscribe(mockObserver);
            verify(mockObservable).observeOn(AndroidSchedulers.mainThread());
        }

        public void itFailsTheSequenceIfAWrongObserverTypeIsUsed() {
            Observer<Integer> wrongObserver = mock(Observer.class); // plain observers not allowed
            OperationObserveInForeground.observeInForeground(mockObservable).subscribe(wrongObserver);
            verify(mockObserver).onError(any(IllegalArgumentException.class));
        }
    }
}

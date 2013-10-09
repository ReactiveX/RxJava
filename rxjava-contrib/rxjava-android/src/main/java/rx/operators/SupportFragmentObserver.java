package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;


public class SupportFragmentObserver<T> implements Observer<T> {

    private final WeakReference<Observer<T>> wrappedRef;
    private final WeakReference<Fragment> fragmentRef;

    public <ObserverT extends Fragment & Observer<T>> SupportFragmentObserver(ObserverT wrapped) {
        this(new WeakReference<Observer<T>>(wrapped), new WeakReference<Fragment>(wrapped));
    }

    public <ObserverT extends SupportFragmentAware & Observer<T>> SupportFragmentObserver(ObserverT wrapped) {
        this(new WeakReference<Observer<T>>(wrapped), new WeakReference<Fragment>(wrapped.getFragment()));
    }

    SupportFragmentObserver(final WeakReference<Observer<T>> observerRef,
                            final WeakReference<Fragment> fragmentRef) {
        this.wrappedRef = observerRef;
        this.fragmentRef = fragmentRef;
    }

    @Override
    public void onCompleted() {
        final Observer<T> target = wrappedRef.get();
        final Fragment fragment = fragmentRef.get();
        if (forwardMessage(target, fragment)) {
            target.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
        final Observer<T> target = wrappedRef.get();
        final Fragment fragment = fragmentRef.get();
        if (forwardMessage(target, fragment)) {
            target.onError(e);
        }
    }

    @Override
    public void onNext(T args) {
        final Observer<T> target = wrappedRef.get();
        final Fragment fragment = fragmentRef.get();
        if (forwardMessage(target, fragment)) {
            target.onNext(args);
        }
    }

    private boolean forwardMessage(final Observer<T> target, final Fragment fragment) {
        return (target != null && fragment != null && fragment.isAdded());
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE)
    public static final class UnitTest {

        private interface WrappedObserverType extends Observer<Integer>, SupportFragmentAware {}

        @Mock
        private WrappedObserverType mockObserver;

        @Mock
        private Fragment mockFragment;

        @Mock
        private Observable<Integer> mockObservable;

        @Before
        public void setupMocks() {
            MockitoAnnotations.initMocks(this);
            when(mockFragment.isAdded()).thenReturn(true);
            when(mockObserver.getFragment()).thenReturn(mockFragment);
        }

        @Test
        public void itForwardsOnNextOnCompletedSequenceToTargetObserver() {
            Observable<Integer> source = Observable.from(1, 2, 3);
            source.subscribe(new SupportFragmentObserver<Integer>(mockObserver));
            verify(mockObserver, times(3)).onNext(anyInt());
            verify(mockObserver).onCompleted();
            verify(mockObserver, never()).onError(any(Exception.class));
        }

        @Test
        public void itForwardsOnErrorToTargetObserver() {
            final Exception exception = new Exception();
            Observable<Integer> source = Observable.error(exception);
            source.subscribe(new SupportFragmentObserver<Integer>(mockObserver));
            verify(mockObserver).onError(exception);
            verify(mockObserver, never()).onNext(anyInt());
            verify(mockObserver, never()).onCompleted();
        }

        @Test
        public void itDropsOnNextOnCompletedSequenceIfFragmentIsGone() {
            PublishSubject<Integer> source = PublishSubject.create();
            WeakReference<Fragment> fragmentRef = new WeakReference<Fragment>(mockFragment);

            source.subscribe(new SupportFragmentObserver<Integer>(
                    new WeakReference<Observer<Integer>>(mockObserver), fragmentRef));

            source.onNext(1);
            fragmentRef.clear();

            source.onNext(2);
            source.onNext(3);
            source.onCompleted();

            verify(mockObserver).onNext(1);
            verifyNoMoreInteractions(mockObserver);
        }

        @Test
        public void itDropsOnNextOnCompletedSequenceIfObserverIsGone() {
            PublishSubject<Integer> source = PublishSubject.create();
            WeakReference<Observer<Integer>> observerRef = new WeakReference<Observer<Integer>>(mockObserver);

            source.subscribe(new SupportFragmentObserver<Integer>(
                    observerRef, new WeakReference<Fragment>(mockFragment)));

            source.onNext(1);
            observerRef.clear();

            source.onNext(2);
            source.onNext(3);
            source.onCompleted();

            verify(mockObserver).onNext(1);
            verifyNoMoreInteractions(mockObserver);
        }

        @Test
        public void itDropsOnErrorIfFragmentIsGone() {
            PublishSubject<Integer> source = PublishSubject.create();
            WeakReference<Fragment> fragmentRef = new WeakReference<Fragment>(mockFragment);

            source.subscribe( new SupportFragmentObserver<Integer>(
                    new WeakReference<Observer<Integer>>(mockObserver), fragmentRef));

            source.onNext(1);
            fragmentRef.clear();

            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verifyNoMoreInteractions(mockObserver);
        }

        @Test
        public void itDropsOnErrorIfSourceObserverIsGone() {
            PublishSubject<Integer> source = PublishSubject.create();
            WeakReference<Observer<Integer>> observerRef = new WeakReference<Observer<Integer>>(mockObserver);

            source.subscribe(new SupportFragmentObserver<Integer>(
                    observerRef, new WeakReference<Fragment>(mockFragment)));

            source.onNext(1);
            observerRef.clear();

            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verifyNoMoreInteractions(mockObserver);
        }

        @Test
        public void itDoesNotForwardOnNextOnCompletedSequenceIfFragmentIsDetached() {
            PublishSubject<Integer> source = PublishSubject.create();
            source.subscribe(new SupportFragmentObserver<Integer>(mockObserver));

            source.onNext(1);

            when(mockFragment.isAdded()).thenReturn(false);
            source.onNext(2);
            source.onNext(3);
            source.onCompleted();

            verify(mockObserver).onNext(1);
            verify(mockObserver, never()).onCompleted();
        }

        @Test
        public void itDoesNotForwardOnErrorIfFragmentIsDetached() {
            PublishSubject<Integer> source = PublishSubject.create();
            source.subscribe(new SupportFragmentObserver<Integer>(mockObserver));

            source.onNext(1);

            when(mockFragment.isAdded()).thenReturn(false);
            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verify(mockObserver, never()).onError(any(Exception.class));
        }


    }

}

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
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.app.Fragment;
import android.util.Log;

import java.lang.ref.WeakReference;

public class OperationObserveInForeground {

    public static <T> Observable<T> observeOnFragment(Observable<T> source, android.app.Fragment fragment) {
        return Observable.create(new OnSubscribeFragment<T>(source, new WeakReference<Fragment>(fragment)));
    }

    public static <T> Observable<T> observeOnSupportFragment(Observable<T> source, android.support.v4.app.Fragment fragment) {
        return Observable.create(new OnSubscribeSupportFragment<T>(
                source, new WeakReference<android.support.v4.app.Fragment>(fragment)));
    }

    public static <T> Observable<T> observeOnActivity(Observable<T> source, Activity activity) {
        return Observable.create(new OnSubscribeActivity<T>(source, new WeakReference<Activity>(activity)));
    }

    private static abstract class OnSubscribeBase<T, AndroidComponent> implements Observable.OnSubscribeFunc<T> {

        private final Observable<T> source;
        private final WeakReference<AndroidComponent> componentRef;
        private Observer<? super T> actual;

        private OnSubscribeBase(Observable<T> source, WeakReference<AndroidComponent> componentRef) {
            this.source = source;
            this.componentRef = componentRef;
        }

        protected abstract boolean isComponentValid(AndroidComponent component);

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            actual = observer;
            return source.observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<T>() {
                @Override
                public void onCompleted() {
                    AndroidComponent component = componentRef.get();
                    if (component != null && isComponentValid(component)) {
                        actual.onCompleted();
                    } else {
                        actual = null;
                        log("onComplete: target component released or detached; dropping message");
                    }
                }

                @Override
                public void onError(Throwable e) {
                    AndroidComponent component = componentRef.get();
                    if (component != null && isComponentValid(component)) {
                        actual.onError(e);
                    } else {
                        actual = null;
                        log("onError: target component released or detached; dropping message");
                    }
                }

                @Override
                public void onNext(T args) {
                    AndroidComponent component = componentRef.get();
                    if (component != null && isComponentValid(component)) {
                        actual.onNext(args);
                    } else {
                        actual = null;
                        log("onNext: target component released or detached; dropping message");
                    }
                }

                private void log(String message) {
                    Log.i(OperationObserveInForeground.class.getSimpleName(), message);
                }
            });
        }
    }

    private static final class OnSubscribeFragment<T> extends OnSubscribeBase<T, android.app.Fragment> {

        private OnSubscribeFragment(Observable<T> source, WeakReference<android.app.Fragment> fragmentRef) {
            super(source, fragmentRef);
        }

        @Override
        protected boolean isComponentValid(android.app.Fragment fragment) {
            return fragment.isAdded();
        }
    }

    private static final class OnSubscribeSupportFragment<T> extends OnSubscribeBase<T, android.support.v4.app.Fragment> {

        private OnSubscribeSupportFragment(Observable<T> source, WeakReference<android.support.v4.app.Fragment> fragment) {
            super(source, fragment);
        }

        @Override
        protected boolean isComponentValid(android.support.v4.app.Fragment fragment) {
            return fragment.isAdded();
        }
    }

    private static final class OnSubscribeActivity<T> extends OnSubscribeBase<T, Activity> {

        private OnSubscribeActivity(Observable<T> source, WeakReference<Activity> activity) {
            super(source, activity);
        }

        @Override
        protected boolean isComponentValid(Activity activity) {
            return !activity.isFinishing();
        }
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE)
    public static final class UnitTest {

        @Mock
        private Observer<Integer> mockObserver;

        @Mock
        private Fragment mockFragment;

        @Mock
        private Activity mockActivity;

        @Mock
        private Observable<Integer> mockObservable;

        @Before
        public void setupMocks() {
            MockitoAnnotations.initMocks(this);
            when(mockFragment.isAdded()).thenReturn(true);
        }

        @Test
        public void itObservesTheSourceSequenceOnTheMainUIThread() {
            OperationObserveInForeground.observeOnFragment(mockObservable, mockFragment).subscribe(mockObserver);
            verify(mockObservable).observeOn(AndroidSchedulers.mainThread());
        }

        @Test
        public void itForwardsOnNextOnCompletedSequenceToTargetObserver() {
            Observable<Integer> source = Observable.from(1, 2, 3);
            OperationObserveInForeground.observeOnFragment(source, mockFragment).subscribe(mockObserver);
            verify(mockObserver, times(3)).onNext(anyInt());
            verify(mockObserver).onCompleted();
            verify(mockObserver, never()).onError(any(Exception.class));
        }

        @Test
        public void itForwardsOnErrorToTargetObserver() {
            final Exception exception = new Exception();
            Observable<Integer> source = Observable.error(exception);
            OperationObserveInForeground.observeOnFragment(source, mockFragment).subscribe(mockObserver);
            verify(mockObserver).onError(exception);
            verify(mockObserver, never()).onNext(anyInt());
            verify(mockObserver, never()).onCompleted();
        }

        @Test
        public void itDropsOnNextOnCompletedSequenceIfTargetComponentIsGone() {
            PublishSubject<Integer> source = PublishSubject.create();
            WeakReference<Fragment> fragmentRef = new WeakReference<Fragment>(mockFragment);

            final OnSubscribeFragment<Integer> operator = new OnSubscribeFragment<Integer>(source, fragmentRef);
            operator.onSubscribe(mockObserver);

            source.onNext(1);
            fragmentRef.clear();

            source.onNext(2);
            source.onNext(3);
            source.onCompleted();

            verify(mockObserver).onNext(1);
            verifyNoMoreInteractions(mockObserver);
        }

        @Test
        public void itDropsOnErrorIfTargetComponentIsGone() {
            PublishSubject<Integer> source = PublishSubject.create();
            WeakReference<Fragment> fragmentRef = new WeakReference<Fragment>(mockFragment);

            final OnSubscribeFragment<Integer> operator = new OnSubscribeFragment<Integer>(source, fragmentRef);
            operator.onSubscribe(mockObserver);

            source.onNext(1);
            fragmentRef.clear();

            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verifyNoMoreInteractions(mockObserver);
        }

        @Test
        public void itDoesNotForwardOnNextOnCompletedSequenceIfFragmentIsDetached() {
            PublishSubject<Integer> source = PublishSubject.create();
            OperationObserveInForeground.observeOnFragment(source, mockFragment).subscribe(mockObserver);

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
            OperationObserveInForeground.observeOnFragment(source, mockFragment).subscribe(mockObserver);

            source.onNext(1);

            when(mockFragment.isAdded()).thenReturn(false);
            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verify(mockObserver, never()).onError(any(Exception.class));
        }

        @Test
        public void isDoesNotForwardOnNextOnCompletedSequenceIfActivityIsFinishing() {
            PublishSubject<Integer> source = PublishSubject.create();
            OperationObserveInForeground.observeOnActivity(source, mockActivity).subscribe(mockObserver);

            source.onNext(1);

            when(mockActivity.isFinishing()).thenReturn(true);
            source.onNext(2);
            source.onNext(3);
            source.onCompleted();

            verify(mockObserver).onNext(1);
            verify(mockObserver, never()).onCompleted();
        }

        @Test
        public void itDoesNotForwardOnErrorIfActivityIsFinishing() {
            PublishSubject<Integer> source = PublishSubject.create();
            OperationObserveInForeground.observeOnActivity(source, mockActivity).subscribe(mockObserver);

            source.onNext(1);

            when(mockActivity.isFinishing()).thenReturn(true);
            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verify(mockObserver, never()).onError(any(Exception.class));
        }
    }
}

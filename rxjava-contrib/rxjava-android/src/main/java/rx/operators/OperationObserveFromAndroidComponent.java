package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
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

import java.lang.reflect.Field;

public class OperationObserveFromAndroidComponent {

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, android.app.Fragment fragment) {
        return Observable.create(new OnSubscribeFragment<T>(source, fragment));
    }

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, android.support.v4.app.Fragment fragment) {
        return Observable.create(new OnSubscribeSupportFragment<T>(source, fragment));
    }

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, Activity activity) {
        return Observable.create(new OnSubscribeActivity<T>(source, activity));
    }

    private static abstract class OnSubscribeBase<T, AndroidComponent> implements Observable.OnSubscribeFunc<T> {

        private static final String LOG_TAG = OperationObserveFromAndroidComponent.class.getSimpleName();

        private final Observable<T> source;
        private volatile AndroidComponent componentRef;
        private volatile Observer<? super T> observerRef;

        private OnSubscribeBase(Observable<T> source, AndroidComponent component) {
            this.source = source;
            this.componentRef = component;
        }

        private void log(String message) {
            Log.d(LOG_TAG, "componentRef = " + componentRef);
            Log.d(LOG_TAG, "observerRef = " + observerRef);
            Log.d(LOG_TAG, message);
        }

        protected abstract boolean isComponentValid(AndroidComponent component);

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            observerRef = observer;
            final Subscription sourceSub = source.observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<T>() {
                @Override
                public void onCompleted() {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onCompleted();
                    } else {
                        log("onComplete: target component released or detached; dropping message");
                        releaseReferences();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onError(e);
                    } else {
                        log("onError: target component released or detached; dropping message");
                        releaseReferences();
                    }
                }

                @Override
                public void onNext(T args) {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onNext(args);
                    } else {
                        log("onNext: target component released or detached; dropping message");
                        releaseReferences();
                    }
                }
            });
            return new Subscription() {
                @Override
                public void unsubscribe() {
                    log("unsubscribing from source sequence");
                    releaseReferences();
                    sourceSub.unsubscribe();
                }
            };
        }

        private void releaseReferences() {
            observerRef = null;
            componentRef = null;
        }
    }

    private static final class OnSubscribeFragment<T> extends OnSubscribeBase<T, android.app.Fragment> {

        private OnSubscribeFragment(Observable<T> source, android.app.Fragment fragment) {
            super(source, fragment);
        }

        @Override
        protected boolean isComponentValid(android.app.Fragment fragment) {
            return fragment.isAdded();
        }
    }

    private static final class OnSubscribeSupportFragment<T> extends OnSubscribeBase<T, android.support.v4.app.Fragment> {

        private OnSubscribeSupportFragment(Observable<T> source, android.support.v4.app.Fragment fragment) {
            super(source, fragment);
        }

        @Override
        protected boolean isComponentValid(android.support.v4.app.Fragment fragment) {
            return fragment.isAdded();
        }
    }

    private static final class OnSubscribeActivity<T> extends OnSubscribeBase<T, Activity> {

        private OnSubscribeActivity(Observable<T> source, Activity activity) {
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
            OperationObserveFromAndroidComponent.observeFromAndroidComponent(mockObservable, mockFragment).subscribe(mockObserver);
            verify(mockObservable).observeOn(AndroidSchedulers.mainThread());
        }

        @Test
        public void itForwardsOnNextOnCompletedSequenceToTargetObserver() {
            Observable<Integer> source = Observable.from(1, 2, 3);
            OperationObserveFromAndroidComponent.observeFromAndroidComponent(source, mockFragment).subscribe(mockObserver);
            verify(mockObserver, times(3)).onNext(anyInt());
            verify(mockObserver).onCompleted();
            verify(mockObserver, never()).onError(any(Exception.class));
        }

        @Test
        public void itForwardsOnErrorToTargetObserver() {
            final Exception exception = new Exception();
            Observable<Integer> source = Observable.error(exception);
            OperationObserveFromAndroidComponent.observeFromAndroidComponent(source, mockFragment).subscribe(mockObserver);
            verify(mockObserver).onError(exception);
            verify(mockObserver, never()).onNext(anyInt());
            verify(mockObserver, never()).onCompleted();
        }

        @Test
        public void itDropsOnNextOnCompletedSequenceIfTargetComponentIsGone() throws Throwable {
            PublishSubject<Integer> source = PublishSubject.create();

            final OnSubscribeFragment<Integer> operator = new OnSubscribeFragment<Integer>(source, mockFragment);
            operator.onSubscribe(mockObserver);

            source.onNext(1);
            releaseComponentRef(operator);

            source.onNext(2);
            source.onNext(3);
            source.onCompleted();

            verify(mockObserver).onNext(1);
            verifyNoMoreInteractions(mockObserver);
        }

        @Test
        public void itDropsOnErrorIfTargetComponentIsGone() throws Throwable {
            PublishSubject<Integer> source = PublishSubject.create();

            final OnSubscribeFragment<Integer> operator = new OnSubscribeFragment<Integer>(source, mockFragment);
            operator.onSubscribe(mockObserver);

            source.onNext(1);
            releaseComponentRef(operator);

            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verifyNoMoreInteractions(mockObserver);
        }

        private void releaseComponentRef(OnSubscribeFragment<Integer> operator) throws NoSuchFieldException, IllegalAccessException {
            final Field componentRef = operator.getClass().getSuperclass().getDeclaredField("componentRef");
            componentRef.setAccessible(true);
            componentRef.set(operator, null);
        }

        @Test
        public void itDoesNotForwardOnNextOnCompletedSequenceIfFragmentIsDetached() {
            PublishSubject<Integer> source = PublishSubject.create();
            OperationObserveFromAndroidComponent.observeFromAndroidComponent(source, mockFragment).subscribe(mockObserver);

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
            OperationObserveFromAndroidComponent.observeFromAndroidComponent(source, mockFragment).subscribe(mockObserver);

            source.onNext(1);

            when(mockFragment.isAdded()).thenReturn(false);
            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verify(mockObserver, never()).onError(any(Exception.class));
        }

        @Test
        public void isDoesNotForwardOnNextOnCompletedSequenceIfActivityIsFinishing() {
            PublishSubject<Integer> source = PublishSubject.create();
            OperationObserveFromAndroidComponent.observeFromAndroidComponent(source, mockActivity).subscribe(mockObserver);

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
            OperationObserveFromAndroidComponent.observeFromAndroidComponent(source, mockActivity).subscribe(mockObserver);

            source.onNext(1);

            when(mockActivity.isFinishing()).thenReturn(true);
            source.onError(new Exception());

            verify(mockObserver).onNext(1);
            verify(mockObserver, never()).onError(any(Exception.class));
        }

        @Test
        public void itUnsubscribesFromTheSourceSequence() {
            Subscription underlying = mock(Subscription.class);
            when(mockObservable.observeOn(AndroidSchedulers.mainThread())).thenReturn(mockObservable);
            when(mockObservable.subscribe(any(Observer.class))).thenReturn(underlying);

            Subscription sub = OperationObserveFromAndroidComponent.observeFromAndroidComponent(
                    mockObservable, mockActivity).subscribe(mockObserver);
            sub.unsubscribe();

            verify(underlying).unsubscribe();
        }
    }
}

/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import android.app.Activity;
import android.app.Fragment;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class OperationObserveFromAndroidComponentTest {

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
    public void itThrowsIfObserverSubscribesFromBackgroundThread() throws Exception {
        final Future<Object> future = Executors.newSingleThreadExecutor().submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                OperationObserveFromAndroidComponent.observeFromAndroidComponent(
                        mockObservable, mockFragment).subscribe(mockObserver);
                return null;
            }
        });
        future.get(1, TimeUnit.SECONDS);
        verify(mockObserver).onError(any(IllegalStateException.class));
        verifyNoMoreInteractions(mockObserver);
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

        final Observable.OnSubscribeFunc<Integer> operator = newOnSubscribeFragmentInstance(source, mockFragment);
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

        final Observable.OnSubscribeFunc<Integer> operator = newOnSubscribeFragmentInstance(source, mockFragment);
        operator.onSubscribe(mockObserver);

        source.onNext(1);
        releaseComponentRef(operator);

        source.onError(new Exception());

        verify(mockObserver).onNext(1);
        verifyNoMoreInteractions(mockObserver);
    }

    private Observable.OnSubscribeFunc<Integer> newOnSubscribeFragmentInstance(Observable<Integer> source, Fragment fragment) throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException  {
        final Class[] klasses = OperationObserveFromAndroidComponent.class.getDeclaredClasses();
        Class onSubscribeFragmentClass = null;
        for (Class klass : klasses) {
            if ("rx.operators.OperationObserveFromAndroidComponent$OnSubscribeFragment".equals(klass.getName())) {
                onSubscribeFragmentClass = klass;
                break;
            }
        }
        Constructor<?> constructor = onSubscribeFragmentClass.getDeclaredConstructor(Observable.class, Fragment.class);
        constructor.setAccessible(true);
        Object object = constructor.newInstance(source, fragment);
        return (Observable.OnSubscribeFunc<Integer>) object;
    }

    private void releaseComponentRef(Observable.OnSubscribeFunc<Integer> operator) throws NoSuchFieldException, IllegalAccessException {
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

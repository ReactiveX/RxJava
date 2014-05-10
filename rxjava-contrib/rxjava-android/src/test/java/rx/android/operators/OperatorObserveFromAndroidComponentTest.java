/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.android.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.operators.OperatorObserveFromAndroidComponent;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import android.app.Fragment;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class OperatorObserveFromAndroidComponentTest {

    @Mock
    private Observer<Integer> mockObserver;

    @Mock
    private Fragment mockFragment;

    @Before
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);
        when(mockFragment.isAdded()).thenReturn(true);
    }

    // TODO needs to be fixed, see comments inline below
    @Ignore
    public void itObservesTheSourceSequenceOnTheMainUIThread() {
        final Observable<Integer> testObservable = Observable.from(1)
                .observeOn(Schedulers.newThread())
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        System.out.println("threadA: " + Thread.currentThread());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        System.out.println("threadB: " + Thread.currentThread());
                    }
                });

        final AtomicReference<String> currentThreadName = new AtomicReference<String>();
        OperatorObserveFromAndroidComponent.observeFromAndroidComponent(testObservable, mockFragment).subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer i) {
                System.out.println("threadV: " + Thread.currentThread());
                currentThreadName.set(Thread.currentThread().getName());
            }
        });

        assertEquals("androidMainThreadName???", currentThreadName.get());

        //TODO Can't use Mockito to validate Observable.observeOn as it is now marked as final.
        //     I can't figure out what to validate about the AndroidSchedulers.mainThread()
        //     as the code above doesn't print `threadB` so I can't see what Thread it should be.
        //     I was going to run it on NewThread then observeOn to AndroidThread and validate it jumped
        //     to the correct thread, but it doesn't do anything. Need to work with Android devs.
    }

    @Test
    public void itForwardsOnNextOnCompletedSequenceToTargetObserver() {
        Observable<Integer> source = Observable.from(Arrays.asList(1, 2, 3));
        OperatorObserveFromAndroidComponent.observeFromAndroidComponent(source, mockFragment).subscribe(new TestObserver<Integer>(mockObserver));
        verify(mockObserver, times(3)).onNext(anyInt());
        verify(mockObserver).onCompleted();
        verify(mockObserver, never()).onError(any(Exception.class));
    }

    @Test
    public void itForwardsOnErrorToTargetObserver() {
        final Exception exception = new Exception();
        Observable<Integer> source = Observable.error(exception);
        OperatorObserveFromAndroidComponent.observeFromAndroidComponent(source, mockFragment).subscribe(new TestObserver<Integer>(mockObserver));
        verify(mockObserver).onError(exception);
        verify(mockObserver, never()).onNext(anyInt());
        verify(mockObserver, never()).onCompleted();
    }

    @Test
    public void itDropsOnNextOnCompletedSequenceIfTargetComponentIsGone() throws Throwable {
        PublishSubject<Integer> source = PublishSubject.create();

        final Observable.OnSubscribe<Integer> operator = newOnSubscribeFragmentInstance(source, mockFragment);
        operator.call(new TestSubscriber<Integer>(mockObserver));

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

        final Observable.OnSubscribe<Integer> operator = newOnSubscribeFragmentInstance(source, mockFragment);
        operator.call(new TestSubscriber<Integer>(mockObserver));

        source.onNext(1);
        releaseComponentRef(operator);

        source.onError(new Exception());

        verify(mockObserver).onNext(1);
        verifyNoMoreInteractions(mockObserver);
    }

    @SuppressWarnings("unchecked")
    private Observable.OnSubscribe<Integer> newOnSubscribeFragmentInstance(Observable<Integer> source, Fragment fragment) throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        final Class<?>[] klasses = OperatorObserveFromAndroidComponent.class.getDeclaredClasses();
        Class<?> onSubscribeFragmentClass = null;
        for (Class<?> klass : klasses) {
            if ("rx.operators.OperatorObserveFromAndroidComponent$OnSubscribeFragment".equals(klass.getName())) {
                onSubscribeFragmentClass = klass;
                break;
            }
        }
        Constructor<?> constructor = onSubscribeFragmentClass.getDeclaredConstructor(Observable.class, Fragment.class);
        constructor.setAccessible(true);
        Object object = constructor.newInstance(source, fragment);
        return (Observable.OnSubscribe<Integer>) object;
    }

    private void releaseComponentRef(Observable.OnSubscribe<Integer> operator) throws NoSuchFieldException, IllegalAccessException {
        final Field componentRef = operator.getClass().getSuperclass().getDeclaredField("componentRef");
        componentRef.setAccessible(true);
        componentRef.set(operator, null);
    }

    @Test
    public void itDoesNotForwardOnNextOnCompletedSequenceIfFragmentIsDetached() {
        PublishSubject<Integer> source = PublishSubject.create();
        OperatorObserveFromAndroidComponent.observeFromAndroidComponent(source, mockFragment).subscribe(new TestObserver<Integer>(mockObserver));

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
        OperatorObserveFromAndroidComponent.observeFromAndroidComponent(source, mockFragment).subscribe(new TestObserver<Integer>(mockObserver));

        source.onNext(1);

        when(mockFragment.isAdded()).thenReturn(false);
        source.onError(new Exception());

        verify(mockObserver).onNext(1);
        verify(mockObserver, never()).onError(any(Exception.class));
    }

}

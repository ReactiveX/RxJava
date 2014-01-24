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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import android.app.Activity;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.ViewObservable;
import rx.observers.TestObserver;

@RunWith(RobolectricTestRunner.class)
public class OperatorViewClickTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testWithoutInitialValue() {
        final View view = new View(Robolectric.buildActivity(Activity.class).create().get());
        final Observable<View> observable = ViewObservable.clicks(view, false);
        final Observer<View> observer = mock(Observer.class);
        final Subscription subscription = observable.subscribe(new TestObserver<View>(observer));

        final InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onNext(any(View.class));

        view.performClick();
        inOrder.verify(observer, times(1)).onNext(view);

        view.performClick();
        inOrder.verify(observer, times(1)).onNext(view);

        subscription.unsubscribe();
        inOrder.verify(observer, never()).onNext(any(View.class));

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithInitialValue() {
        final View view = new View(Robolectric.buildActivity(Activity.class).create().get());
        final Observable<View> observable = ViewObservable.clicks(view, true);
        final Observer<View> observer = mock(Observer.class);
        final Subscription subscription = observable.subscribe(new TestObserver<View>(observer));

        final InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext(view);

        view.performClick();
        inOrder.verify(observer, times(1)).onNext(view);

        view.performClick();
        inOrder.verify(observer, times(1)).onNext(view);

        subscription.unsubscribe();
        inOrder.verify(observer, never()).onNext(any(View.class));

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMultipleSubscriptions() {
        final View view = new View(Robolectric.buildActivity(Activity.class).create().get());
        final Observable<View> observable = ViewObservable.clicks(view, false);

        final Observer<View> observer1 = mock(Observer.class);
        final Observer<View> observer2 = mock(Observer.class);

        final Subscription subscription1 = observable.subscribe(new TestObserver<View>(observer1));
        final Subscription subscription2 = observable.subscribe(new TestObserver<View>(observer2));

        final InOrder inOrder1 = inOrder(observer1);
        final InOrder inOrder2 = inOrder(observer2);

        view.performClick();
        inOrder1.verify(observer1, times(1)).onNext(view);
        inOrder2.verify(observer2, times(1)).onNext(view);

        view.performClick();
        inOrder1.verify(observer1, times(1)).onNext(view);
        inOrder2.verify(observer2, times(1)).onNext(view);
        subscription1.unsubscribe();

        view.performClick();
        inOrder1.verify(observer1, never()).onNext(any(View.class));
        inOrder2.verify(observer2, times(1)).onNext(view);
        subscription2.unsubscribe();

        view.performClick();
        inOrder1.verify(observer1, never()).onNext(any(View.class));
        inOrder2.verify(observer2, never()).onNext(any(View.class));

        inOrder1.verify(observer1, never()).onError(any(Throwable.class));
        inOrder2.verify(observer2, never()).onError(any(Throwable.class));

        inOrder1.verify(observer1, never()).onCompleted();
        inOrder2.verify(observer2, never()).onCompleted();
    }
}

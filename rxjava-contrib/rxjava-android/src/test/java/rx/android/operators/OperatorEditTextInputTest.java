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
import android.widget.EditText;
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
public class OperatorEditTextInputTest {

    private static EditText createEditText(final String value) {
        final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        final EditText text = new EditText(activity);

        if (value != null) {
            text.setText(value);
        }

        return text;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithoutInitialValue() {
        final EditText input = createEditText("initial");
        final Observable<String> observable = ViewObservable.input(input, false);
        final Observer<String> observer = mock(Observer.class);
        final Subscription subscription = observable.subscribe(new TestObserver<String>(observer));

        final InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onNext(anyString());

        input.setText("1");
        inOrder.verify(observer, times(1)).onNext("1");

        input.setText("2");
        inOrder.verify(observer, times(1)).onNext("2");

        input.setText("3");
        inOrder.verify(observer, times(1)).onNext("3");

        subscription.unsubscribe();
        input.setText("4");
        inOrder.verify(observer, never()).onNext(anyString());

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithInitialValue() {
        final EditText input = createEditText("initial");
        final Observable<String> observable = ViewObservable.input(input, true);
        final Observer<String> observer = mock(Observer.class);
        final Subscription subscription = observable.subscribe(new TestObserver<String>(observer));

        final InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext("initial");

        input.setText("one");
        inOrder.verify(observer, times(1)).onNext("one");

        input.setText("two");
        inOrder.verify(observer, times(1)).onNext("two");

        input.setText("three");
        inOrder.verify(observer, times(1)).onNext("three");

        subscription.unsubscribe();
        input.setText("four");
        inOrder.verify(observer, never()).onNext(anyString());

        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMultipleSubscriptions() {
        final EditText input = createEditText("initial");
        final Observable<String> observable = ViewObservable.input(input, false);

        final Observer<String> observer1 = mock(Observer.class);
        final Observer<String> observer2 = mock(Observer.class);

        final Subscription subscription1 = observable.subscribe(new TestObserver<String>(observer1));
        final Subscription subscription2 = observable.subscribe(new TestObserver<String>(observer2));

        final InOrder inOrder1 = inOrder(observer1);
        final InOrder inOrder2 = inOrder(observer2);

        input.setText("1");
        inOrder1.verify(observer1, times(1)).onNext("1");
        inOrder2.verify(observer2, times(1)).onNext("1");

        input.setText("2");
        inOrder1.verify(observer1, times(1)).onNext("2");
        inOrder2.verify(observer2, times(1)).onNext("2");
        subscription1.unsubscribe();

        input.setText("3");
        inOrder1.verify(observer1, never()).onNext(anyString());
        inOrder2.verify(observer2, times(1)).onNext("3");
        subscription2.unsubscribe();

        input.setText("4");
        inOrder1.verify(observer1, never()).onNext(anyString());
        inOrder2.verify(observer2, never()).onNext(anyString());

        inOrder1.verify(observer1, never()).onError(any(Throwable.class));
        inOrder2.verify(observer2, never()).onError(any(Throwable.class));

        inOrder1.verify(observer1, never()).onCompleted();
        inOrder2.verify(observer2, never()).onCompleted();
    }
}

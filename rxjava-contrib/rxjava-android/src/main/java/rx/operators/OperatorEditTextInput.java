/**
 * Copyright 2014 Netflix, Inc.
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

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.observables.Assertions;
import rx.android.subscriptions.AndroidSubscriptions;
import rx.functions.Action0;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class OperatorEditTextInput implements Observable.OnSubscribe<String> {
    private final EditText input;
    private final boolean emitInitialValue;

    public OperatorEditTextInput(final EditText input, final boolean emitInitialValue) {
        this.input = input;
        this.emitInitialValue = emitInitialValue;
    }

    @Override
    public void call(final Subscriber<? super String> observer) {
        Assertions.assertUiThread();
        final TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(final Editable editable) {
                observer.onNext(editable.toString());
            }
        };
        
        final Subscription subscription = AndroidSubscriptions.unsubscribeInUiThread(new Action0() {
            @Override
            public void call() {
                input.removeTextChangedListener(watcher);
            }
        });

        if (emitInitialValue) {
            observer.onNext(input.getEditableText().toString());
        }

        input.addTextChangedListener(watcher);
        observer.add(subscription);
    }

    private static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(final CharSequence sequence, final int start, final int count, final int after) {
            // nothing to do
        }

        @Override
        public void onTextChanged(final CharSequence sequence, final int start, final int before, final int count) {
            // nothing to do
        }

        @Override
        public void afterTextChanged(final Editable editable) {
            // nothing to do
        }
    }
}


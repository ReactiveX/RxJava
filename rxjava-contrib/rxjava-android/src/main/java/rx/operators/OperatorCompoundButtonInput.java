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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.observables.Assertions;
import rx.android.subscriptions.AndroidSubscriptions;
import rx.functions.Action0;
import android.view.View;
import android.widget.CompoundButton;

public class OperatorCompoundButtonInput implements Observable.OnSubscribe<Boolean> {
    private final boolean emitInitialValue;
    private final CompoundButton button;

    public OperatorCompoundButtonInput(final CompoundButton button, final boolean emitInitialValue) {
        this.emitInitialValue = emitInitialValue;
        this.button = button;
    }

    @Override
    public void call(final Subscriber<? super Boolean> observer) {
        Assertions.assertUiThread();
        final CompositeOnCheckedChangeListener composite = CachedListeners.getFromViewOrCreate(button);

        final CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton button, final boolean checked) {
                observer.onNext(checked);
            }
        };

        final Subscription subscription = AndroidSubscriptions.unsubscribeInUiThread(new Action0() {
            @Override
            public void call() {
                composite.removeOnCheckedChangeListener(listener);
            }
        });

        if (emitInitialValue) {
            observer.onNext(button.isChecked());
        }

        composite.addOnCheckedChangeListener(listener);
        observer.add(subscription);
    }

    private static class CompositeOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        private final List<CompoundButton.OnCheckedChangeListener> listeners = new ArrayList<CompoundButton.OnCheckedChangeListener>();

        public boolean addOnCheckedChangeListener(final CompoundButton.OnCheckedChangeListener listener) {
            return listeners.add(listener);
        }

        public boolean removeOnCheckedChangeListener(final CompoundButton.OnCheckedChangeListener listener) {
            return listeners.remove(listener);
        }

        @Override
        public void onCheckedChanged(final CompoundButton button, final boolean checked) {
            for (final CompoundButton.OnCheckedChangeListener listener : listeners) {
                listener.onCheckedChanged(button, checked);
            }
        }
    }

    private static class CachedListeners {
        private static final Map<View, CompositeOnCheckedChangeListener> sCachedListeners = new WeakHashMap<View, CompositeOnCheckedChangeListener>();

        public static CompositeOnCheckedChangeListener getFromViewOrCreate(final CompoundButton button) {
            final CompositeOnCheckedChangeListener cached = sCachedListeners.get(button);

            if (cached != null) {
                return cached;
            }

            final CompositeOnCheckedChangeListener listener = new CompositeOnCheckedChangeListener();

            sCachedListeners.put(button, listener);
            button.setOnCheckedChangeListener(listener);

            return listener;
        }
    }
}



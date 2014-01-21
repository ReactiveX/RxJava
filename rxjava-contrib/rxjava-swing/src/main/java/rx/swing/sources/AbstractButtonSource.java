/**
 * Copyright 2013 Netflix, Inc.
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
package rx.swing.sources;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public enum AbstractButtonSource { ; // no instances

    /**
     * @see rx.observables.SwingObservable#fromButtonAction
     */
    public static Observable<ActionEvent> fromActionOf(final AbstractButton button) {
        return Observable.create(new OnSubscribeFunc<ActionEvent>() {
            @Override
            public Subscription onSubscribe(final Observer<? super ActionEvent> observer) {
                final ActionListener listener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        observer.onNext(e);
                    }
                };
                button.addActionListener(listener);
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        button.removeActionListener(listener);
                    }
                });
            }
        });
    }

}

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
package rx.subjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Notification;
import rx.Observer;

/**
 * Subject that retains all events and will replay them to an {@link Observer} that subscribes.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.ReplaySubject.png">
 * <p>
 * Example usage:
 * <p>
 * 
 * <pre>
 * {
 *     &#064;code
 *     eplaySubject&lt;Object&gt; subject = ReplaySubject.create();
 *     subject.onNext(&quot;one&quot;);
 *     subject.onNext(&quot;two&quot;);
 *     subject.onNext(&quot;three&quot;);
 *     subject.onCompleted();
 * 
 *     // both of the following will get the onNext/onCompleted calls from above
 *     subject.subscribe(observer1);
 *     subject.subscribe(observer2);
 * 
 * }
 * </pre>
 * 
 * @param <T>
 */
public final class ReplaySubject<T> extends Subject<T, T>
{
    private final Map<Object, Observer<? super T>> subscriptions = new HashMap<Object, Observer<? super T>>();
    private final List<Notification<T>> history = new ArrayList<Notification<T>>();

    public static <T> ReplaySubject<T> create() {
        return new ReplaySubject<T>(new ReplayOnGetSubscription<T>());
    }

    private ReplaySubject(ReplayOnGetSubscription<T> onGetSubscription) {
        super(onGetSubscription);
        onGetSubscription.history = history;
        onGetSubscription.subscriptions = subscriptions;
    }

    private static final class ReplayOnGetSubscription<T> implements OnGetSubscriptionFunc<T> {
        private Map<Object, Observer<? super T>> subscriptions;
        private List<Notification<T>> history;

        @Override
        public PartialSubscription<T> onGetSubscription() {
            final Object marker = new Object();
            return PartialSubscription.create(new OnPartialSubscribeFunc<T>() {
                @Override
                public void onSubscribe(Observer<? super T> observer) {
                    int item = 0;

                    for (;;) {
                        while (item < history.size()) {
                            history.get(item++).accept(observer);
                        }

                        synchronized (subscriptions) {
                            if (item < history.size()) {
                                continue;
                            }
                            subscriptions.put(marker, observer);
                            break;
                        }
                    }
                }
            }, new OnPartialUnsubscribeFunc() {
                @Override
                public void onUnsubscribe() {
                    subscriptions.remove(marker);
                }
            });
        }
    }

    @Override
    public void onCompleted() {
        propgate(new Notification<T>());
    }

    @Override
    public void onError(Throwable e) {
        propgate(new Notification<T>(e));
    }

    @Override
    public void onNext(T args) {
        propgate(new Notification<T>(args));
    }

    public void propgate(Notification<T> n) {
        synchronized (subscriptions) {
            history.add(n);
            for (Observer<? super T> observer : new ArrayList<Observer<? super T>>(subscriptions.values())) {
                n.accept(observer);
            }
        }
    }
}

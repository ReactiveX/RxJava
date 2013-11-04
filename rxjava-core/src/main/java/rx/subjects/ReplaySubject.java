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

import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.util.*;

/**
 * Subject that retains all events and will replay them to an {@link Observer} that subscribes.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.ReplaySubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  ReplaySubject<Object> subject = ReplaySubject.create();
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");
  subject.onCompleted();

  // both of the following will get the onNext/onCompleted calls from above
  subject.subscribe(observer1);
  subject.subscribe(observer2);

  } </pre>
 *
 * @param <T>
 */
public final class ReplaySubject<T> extends Subject<T, T>
{

    private boolean isDone = false;
    private Throwable exception = null;
    private final Map<Subscription, Observer<? super T>> subscriptions = new HashMap<Subscription, Observer<? super T>>();
    private final List<T> history = Collections.synchronizedList(new ArrayList<T>());

    public static <T> ReplaySubject<T> create() {
        return new ReplaySubject<T>(new DelegateSubscriptionFunc<T>());
    }

    private ReplaySubject(DelegateSubscriptionFunc<T> onSubscribe) {
        super(onSubscribe);
        onSubscribe.wrap(new SubscriptionFunc());
    }

    private static final class DelegateSubscriptionFunc<T> implements OnSubscribeFunc<T>
    {
        private Func1<? super Observer<? super T>, ? extends Subscription> delegate = null;

        public void wrap(Func1<? super Observer<? super T>, ? extends Subscription> delegate)
        {
            if (this.delegate != null) {
                throw new UnsupportedOperationException("delegate already set");
            }
            this.delegate = delegate;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer)
        {
            return delegate.call(observer);
        }
    }

    private class SubscriptionFunc implements Func1<Observer<? super T>, Subscription>
    {
        @Override
        public Subscription call(Observer<? super T> observer) {
            int item = 0;
            Subscription subscription;

            for (;;) {
                while (item < history.size()) {
                    observer.onNext(history.get(item++));
                }

                synchronized (subscriptions) {
                    if (item < history.size()) {
                        continue;
                    }

                    if (exception != null) {
                        observer.onError(exception);
                        return Subscriptions.empty();
                    }
                    if (isDone) {
                        observer.onCompleted();
                        return Subscriptions.empty();
                    }

                    subscription = new RepeatSubjectSubscription();
                    subscriptions.put(subscription, observer);
                    break;
                }
            }

            return subscription;
        }
    }

    private class RepeatSubjectSubscription implements Subscription
    {
        @Override
        public void unsubscribe()
        {
            synchronized (subscriptions) {
                subscriptions.remove(this);
            }
        }
    }

    @Override
    public void onCompleted()
    {
        synchronized (subscriptions) {
            isDone = true;
            for (Observer<? super T> observer : new ArrayList<Observer<? super T>>(subscriptions.values())) {
                observer.onCompleted();
            }
            subscriptions.clear();
        }
    }

    @Override
    public void onError(Throwable e)
    {
        synchronized (subscriptions) {
            if (isDone) {
                return;
            }
            isDone = true;
            exception = e;
            for (Observer<? super T> observer : new ArrayList<Observer<? super T>>(subscriptions.values())) {
                observer.onError(e);
            }
            subscriptions.clear();
        }
    }

    @Override
    public void onNext(T args)
    {
        synchronized (subscriptions) {
            history.add(args);
            for (Observer<? super T> observer : new ArrayList<Observer<? super T>>(subscriptions.values())) {
                observer.onNext(args);
            }
        }
    }
}

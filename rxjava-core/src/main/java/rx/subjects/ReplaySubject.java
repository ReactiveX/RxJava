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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import rx.Notification;
import rx.Observer;
import rx.util.functions.Action1;

/**
 * Subject that retains all events and will replay them to an {@link Observer} that subscribes.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.ReplaySubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * eplaySubject<Object> subject = ReplaySubject.create();
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
public final class ReplaySubject<T> extends Subject<T, T> {

    public static <T> ReplaySubject<T> create() {
        final SubjectSubscriptionManager<T> subscriptionManager = new SubjectSubscriptionManager<T>();
        final ReplayState<T> state = new ReplayState<T>();

        OnSubscribeFunc<T> onSubscribe = subscriptionManager.getOnSubscribeFunc(
                /**
                 * This function executes at beginning of subscription.
                 * We want to replay history with the subscribing thread
                 * before the Observer gets registered.
                 * 
                 * This will always run, even if Subject is in terminal state.
                 */
                new Action1<Observer<? super T>>() {

                    @Override
                    public void call(Observer<? super T> o) {
                        // replay history for this observer using the subscribing thread
                        Link<T> last = replayObserverFromLink(state.history.head, o);

                        // now that it is caught up add to observers
                        state.replayState.put(o, last);
                    }
                },
                /**
                 * This function executes if the Subject is terminated.
                 */
                new Action1<Observer<? super T>>() {

                    @Override
                    public void call(Observer<? super T> o) {
                        // we will finish replaying if there is anything left
                        replayObserverFromLink(state.replayState.get(o), o);
                    }
                });

        return new ReplaySubject<T>(onSubscribe, subscriptionManager, state);
    }

    private static class ReplayState<T> {
        // single-producer, multi-consumer
        final History<T> history = new History<T>();
        // each Observer is tracked here for what events they have received
        final ConcurrentHashMap<Observer<? super T>, Link<T>> replayState = new ConcurrentHashMap<Observer<? super T>, Link<T>>();
    }

    private final SubjectSubscriptionManager<T> subscriptionManager;
    private final ReplayState<T> state;

    protected ReplaySubject(OnSubscribeFunc<T> onSubscribe, SubjectSubscriptionManager<T> subscriptionManager, ReplayState<T> state) {
        super(onSubscribe);
        this.subscriptionManager = subscriptionManager;
        this.state = state;
    }

    @Override
    public void onCompleted() {
        subscriptionManager.terminate(new Action1<Collection<Observer<? super T>>>() {

            @Override
            public void call(Collection<Observer<? super T>> observers) {
                state.history.add(new Notification<T>());
                for (Observer<? super T> o : observers) {
                    replayObserverToTail(o);
                }
            }
        });
    }

    @Override
    public void onError(final Throwable e) {
        subscriptionManager.terminate(new Action1<Collection<Observer<? super T>>>() {

            @Override
            public void call(Collection<Observer<? super T>> observers) {
                state.history.add(new Notification<T>(e));
                for (Observer<? super T> o : observers) {
                    replayObserverToTail(o);
                }
            }
        });
    }

    @Override
    public void onNext(T v) {
        state.history.add(new Notification<T>(v));
        for (Observer<? super T> o : subscriptionManager.snapshotOfObservers()) {
            replayObserverToTail(o);
        }
    }

    private void replayObserverToTail(Observer<? super T> observer) {
        Link<T> lastEmittedLink = state.replayState.get(observer);
        if (lastEmittedLink != null) {
            Link<T> l = replayObserverFromLink(lastEmittedLink, observer);
            state.replayState.put(observer, l);
        } else {
            throw new IllegalStateException("failed to find lastEmittedLink for: " + observer);
        }
    }

    private static <T> Link<T> replayObserverFromLink(Link<T> l, Observer<? super T> observer) {
        while (l.nextLink != null) {
            // forward to next link
            l = l.nextLink;
            // emit value on this link
            l.n.accept(observer);
        }

        return l;
    }

    /**
     * NOT thread-safe for multi-writer. Assumes single-writer.
     * Is thread-safe for multi-reader.
     * 
     * @param <T>
     */
    private static class History<T> {
        private final Link<T> head = new Link<T>(null);
        private volatile Link<T> tail = head;

        public Link<T> add(Notification<T> n) {
            tail = tail.addNext(n);
            return tail;
        }
    }

    /**
     * Very simple linked-list so each Observer retains a reference to the last Link they saw
     * and can replay forward down the links.
     * 
     * Mutation of the list is NOT thread-safe as it expects single-writer.
     * 
     * Reading of the list is thread-safe as 'Notification' is final and 'nextLink' is volatile.
     * 
     * @param <T>
     */
    private static class Link<T> {
        private final Notification<T> n;
        private volatile Link<T> nextLink;

        private Link(Notification<T> n) {
            this.n = n;
        }

        public Link<T> addNext(Notification<T> n) {
            if (nextLink != null) {
                throw new IllegalStateException("Link is already set");
            }
            nextLink = new Link<T>(n);
            return nextLink;
        }

    }
}

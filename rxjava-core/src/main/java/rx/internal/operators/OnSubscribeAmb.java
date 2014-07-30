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
package rx.internal.operators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Given multiple {@link Observable}s, propagates the one that first emits an item.
 */
public final class OnSubscribeAmb<T> implements OnSubscribe<T>{

    /**
     * Given two {@link Observable}s, propagates the one that first emits an item.
     *
     * @param o1
     *          the first {@code Observable}
     * @param o2
     *          the second {@code Observable}
     * @return an {@code Observable} that mirrors the one of the source {@code Observable}s that was first to
     *         emit an item
     */
    public static <T> OnSubscribe<T> amb(Observable<? extends T> o1, Observable<? extends T> o2) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        return amb(sources);
    }

    /**
     * Given three {@link Observable}s, propagates the one that first emits an item.
     *
     * @param o1
     *          the first {@code Observable}
     * @param o2
     *          the second {@code Observable}
     * @param o3
     *          the third {@code Observable}
     * @return an {@code Observable} that mirrors the one of the source {@code Observable}s that was first to
     *         emit an item
     */
    public static <T> OnSubscribe<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        return amb(sources);
    }

    /**
     * Given four {@link Observable}s, propagates the one that first emits an item.
     *
     * @param o1
     *          the first {@code Observable}
     * @param o2
     *          the second {@code Observable}
     * @param o3
     *          the third {@code Observable}
     * @param o4
     *          the fourth {@code Observable}
     * @return an {@code Observable} that mirrors the one of the source {@code Observable}s that was first to
     *         emit an item
     */
    public static <T> OnSubscribe<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        return amb(sources);
    }

    /**
     * Given five {@link Observable}s, propagates the one that first emits an item.
     *
     * @param o1
     *          the first {@code Observable}
     * @param o2
     *          the second {@code Observable}
     * @param o3
     *          the third {@code Observable}
     * @param o4
     *          the fourth {@code Observable}
     * @param o5
     *          the fifth {@code Observable}
     * @return an {@code Observable} that mirrors the one of the source {@code Observable}s that was first to
     *         emit an item
     */
    public static <T> OnSubscribe<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        return amb(sources);
    }

    /**
     * Given six {@link Observable}s, propagates the one that first emits an item.
     *
     * @param o1
     *          the first {@code Observable}
     * @param o2
     *          the second {@code Observable}
     * @param o3
     *          the third {@code Observable}
     * @param o4
     *          the fourth {@code Observable}
     * @param o5
     *          the fifth {@code Observable}
     * @param o6
     *          the sixth {@code Observable}
     * @return an {@code Observable} that mirrors the one of the source {@code Observable}s that was first to
     *         emit an item
     */
    public static <T> OnSubscribe<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5, Observable<? extends T> o6) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        return amb(sources);
    }

    /**
     * Given seven {@link Observable}s, propagates the one that first emits an item.
     *
     * @param o1
     *          the first {@code Observable}
     * @param o2
     *          the second {@code Observable}
     * @param o3
     *          the third {@code Observable}
     * @param o4
     *          the fourth {@code Observable}
     * @param o5
     *          the fifth {@code Observable}
     * @param o6
     *          the sixth {@code Observable}
     * @param o7
     *          the seventh {@code Observable}
     * @return an {@code Observable} that mirrors the one of the source {@code Observable}s that was first to
     *         emit an item
     */
    public static <T> OnSubscribe<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5, Observable<? extends T> o6, Observable<? extends T> o7) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        sources.add(o7);
        return amb(sources);
    }

    /**
     * Given eight {@link Observable}s, propagates the one that first emits an item.
     *
     * @param o1
     *          the first {@code Observable}
     * @param o2
     *          the second {@code Observable}
     * @param o3
     *          the third {@code Observable}
     * @param o4
     *          the fourth {@code Observable}
     * @param o5
     *          the fifth {@code Observable}
     * @param o6
     *          the sixth {@code Observable}
     * @param o7
     *          the seventh {@code Observable}
     * @param o8
     *          the eighth {@code Observable}
     * @return an {@code Observable} that mirrors the one of the source {@code Observable}s that was first to
     *         emit an item
     */
    public static <T> OnSubscribe<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5, Observable<? extends T> o6, Observable<? extends T> o7, Observable<? extends T> o8) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        sources.add(o7);
        sources.add(o8);
        return amb(sources);
    }

    /**
     * Given nine {@link Observable}s, propagates the one that first emits an item.
     *
     * @param o1
     *          the first {@code Observable}
     * @param o2
     *          the second {@code Observable}
     * @param o3
     *          the third {@code Observable}
     * @param o4
     *          the fourth {@code Observable}
     * @param o5
     *          the fifth {@code Observable}
     * @param o6
     *          the sixth {@code Observable}
     * @param o7
     *          the seventh {@code Observable}
     * @param o8
     *          the eighth {@code Observable}
     * @param o9
     *          the ninth {@code Observable}
     * @return an {@code Observable} that mirrors the one of the source {@code Observable}s that was first to
     *         emit an item
     */
    public static <T> OnSubscribe<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5, Observable<? extends T> o6, Observable<? extends T> o7, Observable<? extends T> o8, Observable<? extends T> o9) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        sources.add(o7);
        sources.add(o8);
        sources.add(o9);
        return amb(sources);
    }

    /**
     * Given a set of {@link Observable}s, propagates the one that first emits an item.
     *
     * @param sources
     *          an {@code Iterable} of {@code Observable}s
     * @return an {@code Observable} that mirrors the one of the {@code Observable}s in {@code sources} that was
     *         the first to emit an item
     */
    public static <T> OnSubscribe<T> amb(final Iterable<? extends Observable<? extends T>> sources) {
        return new OnSubscribeAmb<T>(sources);
    }

    private static final class AmbSubscriber<T> extends Subscriber<T> {

        private final Subscriber<? super T> subscriber;
        private final Selection<T> selection;

        private AmbSubscriber(long requested, Subscriber<? super T> subscriber, Selection<T> selection) {
            this.subscriber = subscriber;
            this.selection = selection;
            // initial request
            request(requested);
        }

        private final void requestMore(long n) {
            request(n);
        }

        @Override
        public void onNext(T args) {
            if (!isSelected()) {
                return;
            }
            subscriber.onNext(args);
        }

        @Override
        public void onCompleted() {
            if (!isSelected()) {
                return;
            }
            subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            if (!isSelected()) {
                return;
            }
            subscriber.onError(e);
        }

        private boolean isSelected() {
            if (selection.choice.get() == this) {
                // fast-path
                return true;
            } else {
                if (selection.choice.compareAndSet(null, this)) {
                    selection.unsubscribeOthers(this);
                    return true;
                } else {
                    // we lost so unsubscribe ... and force cleanup again due to possible race conditions
                    selection.unsubscribeLosers();
                    return false;
                }
            }
        }
    }

    private static class Selection<T> {
        final AtomicReference<AmbSubscriber<T>> choice = new AtomicReference<AmbSubscriber<T>>();
        final Collection<AmbSubscriber<T>> ambSubscribers = new ConcurrentLinkedQueue<AmbSubscriber<T>>();

        public void unsubscribeLosers() {
            AmbSubscriber<T> winner = choice.get();
            if(winner != null) {
                unsubscribeOthers(winner);
            }
        }
        
        public void unsubscribeOthers(AmbSubscriber<T> notThis) {
            for (AmbSubscriber<T> other : ambSubscribers) {
                if (other != notThis) {
                    other.unsubscribe();
                }
            }
            ambSubscribers.clear();
        }

    }

    private final Iterable<? extends Observable<? extends T>> sources;
    private final Selection<T> selection = new Selection<T>();

    private OnSubscribeAmb(Iterable<? extends Observable<? extends T>> sources) {
        this.sources = sources;
    }

    @Override
    public void call(final Subscriber<? super T> subscriber) {
        subscriber.add(Subscriptions.create(new Action0() {

            @Override
            public void call() {
                if (selection.choice.get() != null) {
                    // there is a single winner so we unsubscribe it
                    selection.choice.get().unsubscribe();
                } 
                // if we are racing with others still existing, we'll also unsubscribe them
                if(!selection.ambSubscribers.isEmpty()) {
                    for (AmbSubscriber<T> other : selection.ambSubscribers) {
                        other.unsubscribe();
                    }
                    selection.ambSubscribers.clear();
                }
            }
            
        }));
        subscriber.setProducer(new Producer() {

            @Override
            public void request(long n) {
                if (selection.choice.get() != null) {
                    // propagate the request to that single Subscriber that won
                    selection.choice.get().requestMore(n);
                } else {
                    for (Observable<? extends T> source : sources) {
                        if (subscriber.isUnsubscribed()) {
                            break;
                        }
                        AmbSubscriber<T> ambSubscriber = new AmbSubscriber<T>(n, subscriber, selection);
                        selection.ambSubscribers.add(ambSubscriber);
                        // possible race condition in previous lines ... a choice may have been made so double check (instead of synchronizing)
                        if (selection.choice.get() != null) {
                            // Already chose one, the rest can be skipped and we can clean up
                            selection.unsubscribeOthers(selection.choice.get());
                            break;
                        }
                        source.unsafeSubscribe(ambSubscriber);
                    }
                }
            }
        });
    }

}

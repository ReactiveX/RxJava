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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

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

        private static final int NONE = -1;

        private final Subscriber<? super T> subscriber;
        private final int index;
        private final AtomicInteger choice;

        private AmbSubscriber(Subscriber<? super T> subscriber, int index, AtomicInteger choice) {
            this.subscriber = subscriber;
            this.choice = choice;
            this.index = index;
        }

        @Override
        public void onNext(T args) {
            if (!isSelected()) {
                unsubscribe();
                return;
            }
            subscriber.onNext(args);
        }

        @Override
        public void onCompleted() {
            if (!isSelected()) {
                unsubscribe();
                return;
            }
            subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            if (!isSelected()) {
                unsubscribe();
                return;
            }
            subscriber.onError(e);
        }

        private boolean isSelected() {
            int ch = choice.get();
            if (ch == NONE) {
                return choice.compareAndSet(NONE, index);
            }
            return ch == index;
        }
    }

    private final Iterable<? extends Observable<? extends T>> sources;

    private OnSubscribeAmb(Iterable<? extends Observable<? extends T>> sources) {
        this.sources = sources;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        AtomicInteger choice = new AtomicInteger(AmbSubscriber.NONE);
        int index = 0;
        for (Observable<? extends T> source : sources) {
            if (subscriber.isUnsubscribed()) {
                break;
            }
            if (choice.get() != AmbSubscriber.NONE) {
                // Already choose someone, the rest Observables can be skipped.
                break;
            }
            AmbSubscriber<T> ambSubscriber = new AmbSubscriber<T>(subscriber, index, choice);
            subscriber.add(ambSubscriber);
            source.unsafeSubscribe(ambSubscriber);
            index++;
        }
    }

}

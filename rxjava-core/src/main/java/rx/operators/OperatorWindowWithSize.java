/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.operators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;

/**
 * Creates windows of values into the source sequence with skip frequency and size bounds.
 * 
 * If skip == size then the windows are non-overlapping, otherwise, windows may overlap
 * or can be discontinuous. The returned Observable sequence is cold and need to be
 * consumed while the window operation is in progress.
 * 
 * <p>Note that this conforms the Rx.NET behavior, but does not match former RxJava
 * behavior, which operated as a regular buffer and mapped its lists to Observables.</p>
 * 
 * @param <T> the value type
 */
public final class OperatorWindowWithSize<T> implements Operator<Observable<T>, T> {
    final int size;
    final int skip;

    public OperatorWindowWithSize(int size, int skip) {
        this.size = size;
        this.skip = skip;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super Observable<T>> child) {
        if (skip == size) {
            return new ExactSubscriber(child);
        }
        return new InexactSubscriber(child);
    }
    /** Subscriber with exact, non-overlapping window bounds. */
    final class ExactSubscriber extends Subscriber<T> {
        final Subscriber<? super Observable<T>> child;
        int count;
        Observer<T> consumer;
        Observable<T> producer;
        public ExactSubscriber(Subscriber<? super Observable<T>> child) {
            super(child);
            this.child = child;
        }

        @Override
        public void onNext(T t) {
            if (count++ % size == 0) {
                if (consumer != null) {
                    consumer.onCompleted();
                }
                createNewWindow();
                child.onNext(producer);
            }
            consumer.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            if (consumer != null) {
                consumer.onError(e);
            }
            child.onError(e);
        }

        @Override
        public void onCompleted() {
            if (consumer != null) {
                consumer.onCompleted();
            }
            child.onCompleted();
        }
        void createNewWindow() {
            final BufferUntilSubscriber<T> bus = BufferUntilSubscriber.create();
            consumer = bus;
            producer = bus;
        }
    }
    /** Subscriber with inexact, possibly overlapping or skipping windows. */
    final class InexactSubscriber extends Subscriber<T> {
        final Subscriber<? super Observable<T>> child;
        int count;
        final List<CountedSubject<T>> chunks;
        public InexactSubscriber(Subscriber<? super Observable<T>> child) {
            this.child = child;
            this.chunks = new LinkedList<CountedSubject<T>>();
        }

        @Override
        public void onNext(T t) {
            if (count++ % skip == 0) {
                CountedSubject<T> cs = createCountedSubject();
                chunks.add(cs);
                child.onNext(cs.producer);
            }
            Iterator<CountedSubject<T>> it = chunks.iterator();
            while (it.hasNext()) {
                CountedSubject<T> cs = it.next();
                cs.consumer.onNext(t);
                if (++cs.count == size) {
                    it.remove();
                    cs.consumer.onCompleted();
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            List<CountedSubject<T>> list = new ArrayList<CountedSubject<T>>(chunks);
            chunks.clear();
            for (CountedSubject<T> cs : list) {
                cs.consumer.onError(e);
            }
            child.onError(e);
        }

        @Override
        public void onCompleted() {
            List<CountedSubject<T>> list = new ArrayList<CountedSubject<T>>(chunks);
            chunks.clear();
            for (CountedSubject<T> cs : list) {
                cs.consumer.onCompleted();
            }
            child.onCompleted();
        }
        CountedSubject<T> createCountedSubject() {
            final BufferUntilSubscriber<T> bus = BufferUntilSubscriber.create();
            return new CountedSubject<T>(bus, bus);
        }
    }
    /** 
     * Record to store the subject and the emission count. 
     * @param <T> the subject's in-out type
     */
    static final class CountedSubject<T> {
        final Observer<T> consumer;
        final Observable<T> producer;
        int count;

        public CountedSubject(Observer<T> consumer, Observable<T> producer) {
            this.consumer = consumer;
            this.producer = producer;
        }
    }
}

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
package rx.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.IObservable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Skips any emitted source items as long as the specified condition holds true. Emits all further source items
 * as soon as the condition becomes false.
 */
public final class OperationSkipWhile {
    public static <T> IObservable<T> skipWhileWithIndex(IObservable<? extends T> source, Func2<? super T, Integer, Boolean> predicate) {
        return new SkipWhile<T>(source, predicate);
    }

    public static <T> IObservable<T> skipWhile(IObservable<? extends T> source, final Func1<? super T, Boolean> predicate) {
        return new SkipWhile<T>(source, new Func2<T, Integer, Boolean>() {
            @Override
            public Boolean call(T value, Integer index) {
                return predicate.call(value);
            }
        });
    }

    private static class SkipWhile<T> implements IObservable<T> {
        private final IObservable<? extends T> source;
        private final Func2<? super T, Integer, Boolean> predicate;
        private final AtomicBoolean skipping = new AtomicBoolean(true);
        private final AtomicInteger index = new AtomicInteger(0);

        SkipWhile(IObservable<? extends T> source, Func2<? super T, Integer, Boolean> pred) {
            this.source = source;
            this.predicate = pred;
        }

        @Override
        public Subscription subscribe(Observer<? super T> observer) {
            return source.subscribe(new SkipWhileObserver(observer));
        }

        private class SkipWhileObserver implements Observer<T> {
            private final Observer<? super T> observer;

            public SkipWhileObserver(Observer<? super T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(T next) {
                /* Don't catch exceptions and invoke onError()! We're wrapping
                 * another observable, and that one won't know we handled the
                 * error: it could continue to emit elements when it shouldn't.
                 * Instead, let it catch the error and invoke onError() on this
                 * Observer, and we will pass the call along to our delegate
                 * Observer.
                 */
                if (!skipping.get()) {
                    observer.onNext(next);
                } else if (!predicate.call(next, index.getAndIncrement())) {
                    skipping.set(false);
                    observer.onNext(next);
                }
            }

        }

    }
}

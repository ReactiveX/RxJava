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
package rx.operators;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;

/**
 * Delays the emission of onNext events by a given amount of time.
 * @param <T> the value type
 */
public final class OperatorDelay<T> implements OnSubscribe<T> {

    final Observable<? extends T> source;
    final long delay;
    final TimeUnit unit;
    final Scheduler scheduler;

    public OperatorDelay(Observable<? extends T> source, long delay, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public void call(Subscriber<? super T> child) {
        final Worker worker = scheduler.createWorker();
        child.add(worker);
        
        Observable.concat(source.map(new Func1<T, Observable<T>>() {
            @Override
            public Observable<T> call(T x) {
                Emitter<T> e = new Emitter<T>(x);
                worker.schedule(e, delay, unit);
                return Observable.create(e);
            }
        })).subscribe(child);
    }
    
    /**
     * Emits a value once the call() is invoked.
     * Only one subscriber can wait for the emission.
     * @param <T> the value type
     */
    public static final class Emitter<T> implements OnSubscribe<T>, Action0 {
        final T value;
        
        final Object guard;
        /** Guarded by guard. */
        Subscriber<? super T> child;
        /** Guarded by guard. */
        boolean done;

        public Emitter(T value) {
            this.value = value;
            this.guard = new Object();
        }
        
        @Override
        public void call(Subscriber<? super T> s) {
            synchronized (guard) {
                if (!done) {
                    child = s;
                    return;
                }
            }
            s.onNext(value);
            s.onCompleted();
        }

        @Override
        public void call() {
            Subscriber<? super T> s;
            synchronized (guard) {
                done = true;
                s = child;
                child = null;
            }
            if (s != null) {
                s.onNext(value);
                s.onCompleted();
            }
        }
    }
}

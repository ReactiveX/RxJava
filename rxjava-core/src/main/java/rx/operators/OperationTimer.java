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

import java.util.concurrent.TimeUnit;

import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

public final class OperationTimer {

	public static OnSubscribeFunc<Void> timer(long interval, TimeUnit unit) {
        return timer(interval, unit, Schedulers.threadPoolForComputation());
    }

    public static OnSubscribeFunc<Void> timer(final long delay, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<Void>() {
            @Override
            public Subscription onSubscribe(Observer<? super Void> observer) {
                return new Timer(delay, unit, scheduler, observer).start();
            }
        };
    }
	
	private static class Timer {
        private final long period;
        private final TimeUnit unit;
        private final Scheduler scheduler;
        private final Observer<? super Void> observer;
        
        private Timer(long period, TimeUnit unit, Scheduler scheduler, Observer<? super Void> observer) {
            this.period = period;
            this.unit = unit;
            this.scheduler = scheduler;
            this.observer = observer;
        }

        public Subscription start() {
            final Subscription s = scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onNext(null);
                    observer.onCompleted();
                }
            }, period, unit);

            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    s.unsubscribe();
                }
            });
        }
    }

}

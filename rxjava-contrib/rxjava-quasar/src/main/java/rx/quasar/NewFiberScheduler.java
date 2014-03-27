/**
 * Copyright 2014 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.quasar;

import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.functions.Action1;

/**
 * Schedules work on a new fiber.
 */
public class NewFiberScheduler extends Scheduler {
    private final static NewFiberScheduler DEFAULT_INSTANCE = new NewFiberScheduler();

    public static NewFiberScheduler getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private final FiberScheduler fiberScheduler;

    public NewFiberScheduler(FiberScheduler fiberScheduler) {
        if(fiberScheduler == null)
            throw new IllegalArgumentException("Fiber scheduler is null");
        if(fiberScheduler == DefaultFiberScheduler.getInstance() && DEFAULT_INSTANCE != null)
            throw new IllegalArgumentException("Fiber scheduler is the default FiberScheduler; use getDefaultInstance()");
        this.fiberScheduler = fiberScheduler;
    }

    private NewFiberScheduler() {
        this(DefaultFiberScheduler.getInstance());
    }

    @Override
    public Subscription schedule(Action1<Scheduler.Inner> action) {
        EventLoopScheduler innerScheduler = new EventLoopScheduler();
        innerScheduler.schedule(action);
        return innerScheduler.innerSubscription;
    }
    
    @Override
    public Subscription schedule(Action1<Scheduler.Inner> action, long delayTime, TimeUnit unit) {
        EventLoopScheduler innerScheduler = new EventLoopScheduler();
        innerScheduler.schedule(action, delayTime, unit);
        return innerScheduler.innerSubscription;
    }

    private class EventLoopScheduler extends Scheduler.Inner implements Subscription {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();

        private EventLoopScheduler() {
        }

        @Override
        public void schedule(final Action1<Scheduler.Inner> action) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return;
            }

            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            Subscription s = Subscriptions.from(new Fiber(fiberScheduler, new SuspendableRunnable() {

                @Override
                public void run() throws SuspendExecution {
                    try {
                        if (innerSubscription.isUnsubscribed()) {
                            return;
                        }
                        action.call(EventLoopScheduler.this);
                    } finally {
                        // remove the subscription now that we're completed
                        Subscription s = sf.get();
                        if (s != null) {
                            innerSubscription.remove(s);
                        }
                    }
                }
            }).start());

            sf.set(s);
            innerSubscription.add(s);
        }

        @Override
        public void schedule(final Action1<Scheduler.Inner> action, final long delayTime, final TimeUnit unit) {
            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();

            Subscription s = Subscriptions.from(new Fiber(fiberScheduler, new SuspendableRunnable() {

                @Override
                public void run() throws InterruptedException, SuspendExecution  {
                    Fiber.sleep(delayTime, unit);
                    try {
                        if (innerSubscription.isUnsubscribed()) {
                            return;
                        }
                        // now that the delay is past schedule the work to be done for real on the UI thread
                        action.call(EventLoopScheduler.this);
                    } finally {
                        // remove the subscription now that we're completed
                        Subscription s = sf.get();
                        if (s != null) {
                            innerSubscription.remove(s);
                        }
                    }
                }
            }).start());

            sf.set(s);
            innerSubscription.add(s);
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }
    }
}

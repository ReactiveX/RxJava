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
package rx.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

public class SchedulersTest {
    static final class RunAction extends AtomicBoolean implements Action0 {
        /** */
        private static final long serialVersionUID = -3148738938700490457L;
        private CountDownLatch startLatch = new CountDownLatch(1);
        private CountDownLatch runLatch = new CountDownLatch(1);
        private CountDownLatch completeLatch = new CountDownLatch(1);
        private volatile boolean waitInterrupted;
        @Override
        public void call() {
            startLatch.countDown();
            try {
                runLatch.await();
            } catch (InterruptedException ex) {
                waitInterrupted = true;
                completeLatch.countDown();
                return;
            }
            lazySet(true);
            completeLatch.countDown();
        }
        private void await(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        public void awaitStart() {
            await(startLatch);
        }
        public void awaitComplete() {
            await(completeLatch);
        }
        public boolean isWaitInterrupted() {
            return waitInterrupted;
        }
        public void run() {
            runLatch.countDown();
        }
    }
    @Test
    public void submitToSimpleInterrupt() {
        RunAction ra = new RunAction();
        
        CompositeSubscription csub = new CompositeSubscription();
        
        ExecutorService exec = Executors.newFixedThreadPool(1);
        try {
            Subscription s = Schedulers.submitTo(exec, ra, csub, true);
            
            ra.awaitStart();
            
            csub.remove(s);

            ra.awaitComplete();
            
            assertTrue(ra.isWaitInterrupted());
            assertFalse(ra.get());
            assertTrue(s.isUnsubscribed());
            
        } finally {
            exec.shutdownNow();
        }
        
    }

    @Test
    public void submitToSimpleNoInterrupt() {
        RunAction ra = new RunAction();
        
        CompositeSubscription csub = new CompositeSubscription();
        
        ExecutorService exec = Executors.newFixedThreadPool(1);
        try {
            Subscription s = Schedulers.submitTo(exec, ra, csub, false);
            
            ra.awaitStart();
            
            csub.remove(s);
            
            ra.run();

            ra.awaitComplete();
            
            assertFalse(ra.isWaitInterrupted());
            assertTrue(ra.get());
            assertTrue(s.isUnsubscribed());
            
        } finally {
            exec.shutdownNow();
        }
        
    }
    @Test
    public void submitToSimpleInterruptNoParent() {
        RunAction ra = new RunAction();
        
        ExecutorService exec = Executors.newFixedThreadPool(1);
        try {
            Subscription s = Schedulers.submitTo(exec, ra, null, true);
            
            ra.awaitStart();
            
            s.unsubscribe();

            ra.awaitComplete();
            
            assertTrue(ra.isWaitInterrupted());
            assertFalse(ra.get());
            assertTrue(s.isUnsubscribed());
            
        } finally {
            exec.shutdownNow();
        }
        
    }

    @Test
    public void submitToSimpleNoInterruptNoParent() {
        RunAction ra = new RunAction();
        
        ExecutorService exec = Executors.newFixedThreadPool(1);
        try {
            Subscription s = Schedulers.submitTo(exec, ra, null, false);
            
            ra.awaitStart();

            s.unsubscribe();
            
            ra.run();

            ra.awaitComplete();
            
            assertFalse(ra.isWaitInterrupted());
            assertTrue(ra.get());
            assertTrue(s.isUnsubscribed());
            
        } finally {
            exec.shutdownNow();
        }
        
    }
    @Test
    public void submitToDelayedSimpleInterrupt() {
        RunAction ra = new RunAction();
        
        CompositeSubscription csub = new CompositeSubscription();
        
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        try {
            Subscription s = Schedulers.submitTo(exec, ra, 500, TimeUnit.MILLISECONDS, csub, true);
            
            ra.awaitStart();
            
            csub.remove(s);

            ra.awaitComplete();
            
            assertTrue(ra.isWaitInterrupted());
            assertFalse(ra.get());
            assertTrue(s.isUnsubscribed());
            
        } finally {
            exec.shutdownNow();
        }
        
    }
    @Test(timeout = 3000)
    public void submitToDelayedSimpleInterruptBeforeRun() throws InterruptedException {
        RunAction ra = new RunAction();
        
        CompositeSubscription csub = new CompositeSubscription();
        
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        try {
            Subscription s = Schedulers.submitTo(exec, ra, 1000, TimeUnit.MILLISECONDS, csub, true);
            
            Thread.sleep(500);
            
            csub.remove(s);

            Thread.sleep(1000);
            
            assertFalse(ra.isWaitInterrupted());
            assertFalse(ra.get());
            assertTrue(s.isUnsubscribed());
        } finally {
            exec.shutdownNow();
        }
        
    }

    @Test
    public void submitToDelayedSimpleNoInterrupt() {
        RunAction ra = new RunAction();
        
        CompositeSubscription csub = new CompositeSubscription();
        
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        
        try {
            Subscription s = Schedulers.submitTo(exec, ra, 500, TimeUnit.MILLISECONDS, csub, false);
            
            ra.awaitStart();
            
            csub.remove(s);
            
            ra.run();

            ra.awaitComplete();
            
            assertFalse(ra.isWaitInterrupted());
            assertTrue(ra.get());
            assertTrue(s.isUnsubscribed());
            
        } finally {
            exec.shutdownNow();
        }
        
    }
    @Test
    public void submitToDelayedSimpleInterruptNoParent() {
        RunAction ra = new RunAction();
        
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        try {
            Subscription s = Schedulers.submitTo(exec, ra, 500, TimeUnit.MILLISECONDS, null, true);
            
            ra.awaitStart();

            s.unsubscribe();

            ra.awaitComplete();
            
            assertTrue(ra.isWaitInterrupted());
            assertFalse(ra.get());
            assertTrue(s.isUnsubscribed());
            
        } finally {
            exec.shutdownNow();
        }
        
    }
    @Test(timeout = 3000)
    public void submitToDelayedSimpleInterruptBeforeRunNoParent() throws InterruptedException {
        RunAction ra = new RunAction();
        
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        try {
            Subscription s = Schedulers.submitTo(exec, ra, 1000, TimeUnit.MILLISECONDS, null, true);
            
            Thread.sleep(500);

            s.unsubscribe();

            Thread.sleep(1000);
            
            assertFalse(ra.isWaitInterrupted());
            assertFalse(ra.get());
            assertTrue(s.isUnsubscribed());
        } finally {
            exec.shutdownNow();
        }
        
    }

    @Test
    public void submitToDelayedSimpleNoInterruptNoParent() {
        RunAction ra = new RunAction();
        
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        
        try {
            Subscription s = Schedulers.submitTo(exec, ra, 500, TimeUnit.MILLISECONDS, null, false);
            
            ra.awaitStart();
            
            s.unsubscribe();
            
            ra.run();

            ra.awaitComplete();
            
            assertFalse(ra.isWaitInterrupted());
            assertTrue(ra.get());
            assertTrue(s.isUnsubscribed());
            
        } finally {
            exec.shutdownNow();
        }
        
    }

}

/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.rxjava3.internal.schedulers;

import io.reactivex.rxjava3.core.RxJavaTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class CompleteScheduledThreadPoolTest extends RxJavaTest {

    @Test
    public void executeImmediately() throws Exception {
        CompleteScheduledThreadPoolExecutor executor = new CompleteScheduledThreadPoolExecutor(1);
        try {
            CountDownLatch latch = new CountDownLatch(4);

            executor.submit(
                    new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            latch.countDown();
                            return "t";
                        }
                    }, new CompleteScheduledExecutorService.CompleteHandler<String>() {
                        @Override
                        public void onComplete(Future<String> task) {
                            try {
                                if ("t".equals(task.get())) {
                                    latch.countDown();
                                }
                            } catch (InterruptedException e) {
                            } catch (ExecutionException e) {
                            }
                        }
                    });

            executor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            latch.countDown();
                        }
                    },
                    "t",
                    new CompleteScheduledExecutorService.CompleteHandler<String>() {
                        @Override
                        public void onComplete(Future task) {
                            try {
                                if ("t".equals(task.get())) {
                                    latch.countDown();
                                }
                            } catch (InterruptedException e) {
                            } catch (ExecutionException e) {
                            }
                        }
                    });

            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void executeDelay() throws Exception {
        CompleteScheduledThreadPoolExecutor executor = new CompleteScheduledThreadPoolExecutor(1);
        try {
            CountDownLatch latch = new CountDownLatch(4);
            long startTime = System.nanoTime();
            long[] delay1 = new long[1];
            long[] delay2 = new long[1];

            executor.schedule(
                    new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            delay1[0] = System.nanoTime() - startTime;
                            latch.countDown();
                            return "t";
                        }
                    },
                    1,
                    TimeUnit.SECONDS,
                    new CompleteScheduledExecutorService.CompleteHandler<String>() {
                        @Override
                        public void onComplete(Future<String> task) {
                            try {
                                if ("t".equals(task.get())) {
                                    latch.countDown();
                                }
                            } catch (InterruptedException e) {
                            } catch (ExecutionException e) {
                            }
                        }
                    });

            executor.schedule(
                    new Runnable() {
                        @Override
                        public void run() {
                            delay2[0] = System.nanoTime() - startTime;
                            latch.countDown();
                        }
                    },
                    "t",
                    1,
                    TimeUnit.SECONDS,
                    new CompleteScheduledExecutorService.CompleteHandler<String>() {
                        @Override
                        public void onComplete(Future task) {
                            try {
                                if ("t".equals(task.get())) {
                                    latch.countDown();
                                }
                            } catch (InterruptedException e) {
                            } catch (ExecutionException e) {
                            }
                        }
                    });

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertTrue(delay1[0] >= TimeUnit.SECONDS.toNanos(1));
            assertTrue(delay2[0] >= TimeUnit.SECONDS.toNanos(1));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void executeAtFixedRate() throws Exception {
        CompleteScheduledThreadPoolExecutor executor = new CompleteScheduledThreadPoolExecutor(1);
        try {
            CountDownLatch latch = new CountDownLatch(1);

            long startTime = System.nanoTime();
            List<Long> delays = new ArrayList<>();

            final RuntimeException endException = new RuntimeException();

            executor.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                            }
                            delays.add(delays.isEmpty() ?
                                    System.nanoTime() - startTime : System.nanoTime() - delays.get(delays.size() - 1));
                            if (delays.size() >= 5) {
                                throw endException;
                            }
                        }
                    },
                    100,
                    500,
                    TimeUnit.MILLISECONDS,
                    new CompleteScheduledExecutorService.CompleteHandler<Void>() {
                        @Override
                        public void onComplete(Future<Void> task) {
                            try {
                                task.get();
                            } catch (InterruptedException e) {
                            } catch (ExecutionException e) {
                                if (endException == e.getCause()) {
                                    latch.countDown();
                                }
                            }
                        }
                    });

            assertTrue(latch.await(4, TimeUnit.SECONDS));
            for (int i = 0; i < delays.size(); ++i) {
                if (i == 0) { assertTrue(delays.get(i) >= TimeUnit.MICROSECONDS.toNanos(100)); }
                else { assertTrue(delays.get(i) >= TimeUnit.MICROSECONDS.toNanos(500)); }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void executeAtFixedDelay() throws Exception {
        CompleteScheduledThreadPoolExecutor executor = new CompleteScheduledThreadPoolExecutor(1);
        try {
            CountDownLatch latch = new CountDownLatch(1);

            long startTime = System.nanoTime();
            List<Long> delays = new ArrayList<>();

            final RuntimeException endException = new RuntimeException();

            executor.scheduleWithFixedDelay(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                            }
                            delays.add(delays.isEmpty() ?
                                    System.nanoTime() - startTime : System.nanoTime() - delays.get(delays.size() - 1));
                            if (delays.size() >= 5) {
                                throw endException;
                            }
                        }
                    },
                    100,
                    200,
                    TimeUnit.MILLISECONDS,
                    new CompleteScheduledExecutorService.CompleteHandler<Void>() {
                        @Override
                        public void onComplete(Future<Void> task) {
                            try {
                                task.get();
                            } catch (InterruptedException e) {
                            } catch (ExecutionException e) {
                                if (endException == e.getCause()) {
                                    latch.countDown();
                                }
                            }
                        }
                    });

            assertTrue(latch.await(4, TimeUnit.SECONDS));
            for (int i = 0; i < delays.size(); ++i) {
                if (i == 0) { assertTrue(delays.get(i) >= TimeUnit.MICROSECONDS.toNanos(100)); }
                else { assertTrue(delays.get(i) >= TimeUnit.MICROSECONDS.toNanos(500)); }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void wouldNotCallCompleteHandlerForCanceledTask() throws Exception {
        CompleteScheduledThreadPoolExecutor executor = new CompleteScheduledThreadPoolExecutor(1);
        try {
            CountDownLatch latch = new CountDownLatch(1);

            Future<?> future = executor.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                        }
                    },
                    10,
                    100,
                    TimeUnit.MILLISECONDS,
                    new CompleteScheduledExecutorService.CompleteHandler<Void>() {
                        @Override
                        public void onComplete(Future<Void> task) {
                            latch.countDown();
                        }
                    });

            future.cancel(true);

            assertFalse(latch.await(3, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void multiThreadPeriodicTaskCompleteTestRace() throws Exception {
        CompleteScheduledThreadPoolExecutor executor = new CompleteScheduledThreadPoolExecutor(6);
        try {
            int total = 1000;

            CountDownLatch latch = new CountDownLatch(total);
            AtomicInteger completeCallCount = new AtomicInteger();

            for (int i = 0; i < total; ++i) {
                Future<?> future = executor.scheduleAtFixedRate(
                        new Runnable() {
                            private int runCount = 0;

                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                }
                                if (6 == runCount++) {
                                    throw new RuntimeException();
                                }
                            }
                        },
                        0,
                        1,
                        TimeUnit.MILLISECONDS,
                        new CompleteScheduledExecutorService.CompleteHandler<Void>() {
                            @Override
                            public void onComplete(Future<Void> task) {
                                latch.countDown();
                                completeCallCount.incrementAndGet();
                            }
                        });
            }

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertEquals(total, completeCallCount.get());
        } finally {
            executor.shutdownNow();
        }
    }
}

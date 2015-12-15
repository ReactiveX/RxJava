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
package rx.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class IndexedRingBufferTest {

    @Test
    public void add() {
        IndexedRingBuffer<LSubscription> list = IndexedRingBuffer.getInstance();
        list.add(new LSubscription(1));
        list.add(new LSubscription(2));
        final AtomicInteger c = new AtomicInteger();

        list.forEach(newCounterAction(c));
        assertEquals(2, c.get());
    }

    @Test
    public void removeEnd() {
        IndexedRingBuffer<LSubscription> list = IndexedRingBuffer.getInstance();
        list.add(new LSubscription(1));
        int n2 = list.add(new LSubscription(2));

        final AtomicInteger c = new AtomicInteger();
        list.forEach(newCounterAction(c));
        assertEquals(2, c.get());

        list.remove(n2);

        final AtomicInteger c2 = new AtomicInteger();
        list.forEach(newCounterAction(c2));
        assertEquals(1, c2.get());
    }

    @Test
    public void removeMiddle() {
        IndexedRingBuffer<LSubscription> list = IndexedRingBuffer.getInstance();
        list.add(new LSubscription(1));
        int n2 = list.add(new LSubscription(2));
        list.add(new LSubscription(3));

        list.remove(n2);

        final AtomicInteger c = new AtomicInteger();
        list.forEach(newCounterAction(c));
        assertEquals(2, c.get());
    }

    @Test
    public void addRemoveAdd() {
        IndexedRingBuffer<String> list = IndexedRingBuffer.getInstance();
        list.add("one");
        list.add("two");
        list.add("three");
        ArrayList<String> values = new ArrayList<String>();
        list.forEach(accumulate(values));
        assertEquals(3, values.size());
        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));

        list.remove(1);

        values.clear();
        list.forEach(accumulate(values));
        assertEquals(2, values.size());
        assertEquals("one", values.get(0));
        assertEquals("three", values.get(1));

        list.add("four");

        values.clear();
        list.forEach(accumulate(values));
        assertEquals(3, values.size());
        assertEquals("one", values.get(0));
        assertEquals("four", values.get(1));
        assertEquals("three", values.get(2));

        final AtomicInteger c = new AtomicInteger();
        list.forEach(newCounterAction(c));
        assertEquals(3, c.get());
    }

    @Test
    public void addThousands() {
        String s = "s";
        IndexedRingBuffer<String> list = IndexedRingBuffer.getInstance();
        for (int i = 0; i < 10000; i++) {
            list.add(s);
        }
        AtomicInteger c = new AtomicInteger();
        list.forEach(newCounterAction(c));
        assertEquals(10000, c.get());

        list.remove(5000);
        c.set(0);
        list.forEach(newCounterAction(c));
        assertEquals(9999, c.get());

        list.add("one");
        list.add("two");
        c.set(0);

        //        list.forEach(print());

        list.forEach(newCounterAction(c));
        assertEquals(10001, c.get());
    }

    @Test
    public void testForEachWithIndex() {
        IndexedRingBuffer<String> buffer = IndexedRingBuffer.getInstance();
        buffer.add("zero");
        buffer.add("one");
        buffer.add("two");
        buffer.add("three");

        final ArrayList<String> list = new ArrayList<String>();
        int nextIndex = buffer.forEach(accumulate(list));
        assertEquals(4, list.size());
        assertEquals(list, Arrays.asList("zero", "one", "two", "three"));
        assertEquals(0, nextIndex);

        list.clear();
        nextIndex = buffer.forEach(accumulate(list), 0);
        assertEquals(4, list.size());
        assertEquals(list, Arrays.asList("zero", "one", "two", "three"));
        assertEquals(0, nextIndex);

        list.clear();
        nextIndex = buffer.forEach(accumulate(list), 2);
        assertEquals(4, list.size());
        assertEquals(list, Arrays.asList("two", "three", "zero", "one"));
        assertEquals(2, nextIndex); // 2, 3, 0, 1

        list.clear();
        nextIndex = buffer.forEach(accumulate(list), 3);
        assertEquals(4, list.size());
        assertEquals(list, Arrays.asList("three", "zero", "one", "two"));
        assertEquals(3, nextIndex); // 3, 0, 1, 2

        list.clear();
        nextIndex = buffer.forEach(new Func1<String, Boolean>() {

            @Override
            public Boolean call(String t1) {
                list.add(t1);
                return false;
            }

        }, 3);
        assertEquals(1, list.size());
        assertEquals(list, Arrays.asList("three"));
        assertEquals(3, nextIndex); // we ended early so we'll go back to this index again next time

        list.clear();
        nextIndex = buffer.forEach(new Func1<String, Boolean>() {
            int i = 0;

            @Override
            public Boolean call(String t1) {
                list.add(t1);
                i++;
                return i != 3;
            }

        }, 0);
        assertEquals(3, list.size());
        assertEquals(list, Arrays.asList("zero", "one", "two"));
        assertEquals(2, nextIndex); // 0, 1, 2 (// we ended early so we'll go back to the last index again next time)
    }

    @Test
    public void testForEachAcrossSections() {
        IndexedRingBuffer<Integer> buffer = IndexedRingBuffer.getInstance();
        for (int i = 0; i < 10000; i++) {
            buffer.add(i);
        }

        final ArrayList<Integer> list = new ArrayList<Integer>();
        int nextIndex = buffer.forEach(accumulate(list), 5000);
        assertEquals(10000, list.size());
        assertEquals(Integer.valueOf(5000), list.get(0));
        assertEquals(Integer.valueOf(9999), list.get(4999));
        assertEquals(Integer.valueOf(0), list.get(5000));
        assertEquals(Integer.valueOf(4999), list.get(9999));
        assertEquals(5000, nextIndex);
    }

    @Test
    public void longRunningAddRemoveAddDoesntLeakMemory() {
        String s = "s";
        IndexedRingBuffer<String> list = IndexedRingBuffer.getInstance();
        for (int i = 0; i < 20000; i++) {
            int index = list.add(s);
            list.remove(index);
        }

        AtomicInteger c = new AtomicInteger();
        list.forEach(newCounterAction(c));
        assertEquals(0, c.get());
        //        System.out.println("Index is: " + list.index.get() + " when it should be no bigger than " + list.SIZE);
        assertTrue(list.index.get() < IndexedRingBuffer.SIZE);
        // it should actually be 1 since we only did add/remove sequentially
        assertEquals(1, list.index.get());
    }

    @Test
    public void testConcurrentAdds() throws InterruptedException {
        final IndexedRingBuffer<Integer> list = IndexedRingBuffer.getInstance();

        Scheduler.Worker w1 = Schedulers.computation().createWorker();
        Scheduler.Worker w2 = Schedulers.computation().createWorker();

        final CountDownLatch latch = new CountDownLatch(2);

        w1.schedule(new Action0() {

            @Override
            public void call() {
                for (int i = 0; i < 10000; i++) {
                    list.add(i);
                }
                latch.countDown();
            }

        });
        w2.schedule(new Action0() {

            @Override
            public void call() {
                for (int i = 10000; i < 20000; i++) {
                    list.add(i);
                }
                latch.countDown();
            }

        });

        latch.await();

        w1.unsubscribe();
        w2.unsubscribe();

        AtomicInteger c = new AtomicInteger();
        list.forEach(newCounterAction(c));
        assertEquals(20000, c.get());

        ArrayList<Integer> values = new ArrayList<Integer>();
        list.forEach(accumulate(values));
        Collections.sort(values);
        int j = 0;
        for (int i : values) {
            assertEquals(i, j++);
        }
    }

    @Test
    public void testConcurrentAddAndRemoves() throws InterruptedException {
        final IndexedRingBuffer<Integer> list = IndexedRingBuffer.getInstance();

        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());

        Scheduler.Worker w1 = Schedulers.computation().createWorker();
        Scheduler.Worker w2 = Schedulers.computation().createWorker();

        final CountDownLatch latch = new CountDownLatch(2);

        w1.schedule(new Action0() {

            @Override
            public void call() {
                try {
                    for (int i = 10000; i < 20000; i++) {
                        list.add(i);
                        //                        Integer v = list.remove(index);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exceptions.add(e);
                }
                latch.countDown();
            }

        });

        w2.schedule(new Action0() {

            @Override
            public void call() {
                try {
                    for (int i = 0; i < 10000; i++) {
                        int index = list.add(i);
                        // cause some random remove/add interference
                        Integer v = list.remove(index);
                        if (v == null) {
                            throw new RuntimeException("should not get null");
                        }
                        list.add(v);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exceptions.add(e);
                }
                latch.countDown();
            }

        });

        latch.await();

        w1.unsubscribe();
        w2.unsubscribe();

        AtomicInteger c = new AtomicInteger();
        list.forEach(newCounterAction(c));
        assertEquals(20000, c.get());

        ArrayList<Integer> values = new ArrayList<Integer>();
        list.forEach(accumulate(values));
        Collections.sort(values);
        int j = 0;
        for (int i : values) {
            assertEquals(i, j++);
        }

        if (exceptions.size() > 0) {
            System.out.println("Exceptions: " + exceptions);
        }
        assertEquals(0, exceptions.size());
    }

    private <T> Func1<T, Boolean> accumulate(final ArrayList<T> list) {
        return new Func1<T, Boolean>() {

            @Override
            public Boolean call(T t1) {
                list.add(t1);
                return true;
            }

        };
    }

    @SuppressWarnings("unused")
    private Func1<Object, Boolean> print() {
        return new Func1<Object, Boolean>() {

            @Override
            public Boolean call(Object t1) {
                System.out.println("Object: " + t1);
                return true;
            }

        };
    }

    private Func1<Object, Boolean> newCounterAction(final AtomicInteger c) {
        return new Func1<Object, Boolean>() {

            @Override
            public Boolean call(Object t1) {
                c.incrementAndGet();
                return true;
            }

        };
    }

    public static class LSubscription implements Subscription {

        private final int n;

        public LSubscription(int n) {
            this.n = n;
        }

        @Override
        public void unsubscribe() {

        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }

        @Override
        public String toString() {
            return "Subscription=>" + n;
        }
    }
}
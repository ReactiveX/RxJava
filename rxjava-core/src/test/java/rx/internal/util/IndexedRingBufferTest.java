package rx.internal.util;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Subscription;
import rx.functions.Action1;

public class IndexedRingBufferTest {

    @Test
    public void add() {
        IndexedRingBuffer<LSubscription> list = IndexedRingBuffer.getInstance();
        int head = list.add(new LSubscription(1));
        int n2 = list.add(new LSubscription(2));
        final AtomicInteger c = new AtomicInteger();

        list.forEach(newCounterAction(c));
        assertEquals(2, c.get());
    }

    @Test
    public void removeEnd() {
        IndexedRingBuffer<LSubscription> list = IndexedRingBuffer.getInstance();
        int head = list.add(new LSubscription(1));
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
        int head = list.add(new LSubscription(1));
        int n2 = list.add(new LSubscription(2));
        int n3 = list.add(new LSubscription(3));

        list.remove(n2);

        final AtomicInteger c = new AtomicInteger();
        list.forEach(newCounterAction(c));
        assertEquals(2, c.get());
    }

    private Action1<LSubscription> newCounterAction(final AtomicInteger c) {
        return new Action1<LSubscription>() {

            @Override
            public void call(LSubscription t1) {
                c.incrementAndGet();
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

package rx.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Subscription;
import rx.functions.Action1;
import rx.internal.util.ConcurrentLinkedNode.Node;

public class ConcurrentLinkedNodeTest {

    @Test
    public void add() {
        ConcurrentLinkedNode<LSubscription> list = new ConcurrentLinkedNode<LSubscription>();
        Node<LSubscription> head = list.add(new LSubscription(1));
        Node<LSubscription> n2 = list.add(new LSubscription(2));
        final AtomicInteger c = new AtomicInteger();

        Iterator<LSubscription> il = list.iteratorStartingAt(head);
        System.out.println(il.hasNext());
        System.out.println(il.next());
        System.out.println(il.hasNext());
        System.out.println(il.next());

        list.forEach(newCounterAction(c));
        assertEquals(2, c.get());
        assertEquals(n2, head.next());
        assertNull(n2.next());
        assertEquals(head, n2.prev());
    }

    @Test
    public void removeEnd() {
        ConcurrentLinkedNode<LSubscription> list = new ConcurrentLinkedNode<LSubscription>();
        Node<LSubscription> head = list.add(new LSubscription(1));
        Node<LSubscription> n2 = list.add(new LSubscription(2));

        final AtomicInteger c = new AtomicInteger();
        list.forEach(head, newCounterAction(c));
        assertEquals(2, c.get());

        list.remove(n2);

        final AtomicInteger c2 = new AtomicInteger();
        list.forEach(head, newCounterAction(c2));
        assertEquals(1, c2.get());

        assertEquals(1, head.get().n);
        assertNull(head.next().item);
        assertNull(head.prev().item);
    }

    @Test
    public void removeMiddle() {
        ConcurrentLinkedNode<LSubscription> list = new ConcurrentLinkedNode<LSubscription>();
        Node<LSubscription> head = list.add(new LSubscription(1));
        Node<LSubscription> n2 = list.add(new LSubscription(2));
        Node<LSubscription> n3 = list.add(new LSubscription(3));

        assertEquals(1, head.get().n);
        assertEquals(2, head.next().get().n);
        assertEquals(3, head.next().next().get().n);

        list.remove(n2);

        final AtomicInteger c = new AtomicInteger();
        list.forEach(head, newCounterAction(c));
        assertEquals(2, c.get());

        assertEquals(1, head.get().n);
        assertEquals(3, head.next().get().n);
    }

    @Test
    public void iterateFromMiddle() {
        ConcurrentLinkedNode<LSubscription> list = new ConcurrentLinkedNode<LSubscription>();
        Node<LSubscription> head = list.add(new LSubscription(1));
        Node<LSubscription> n2 = list.add(new LSubscription(2));
        Node<LSubscription> n3 = list.add(new LSubscription(3));

        final ArrayList<LSubscription> ss = new ArrayList<LSubscription>();

        list.forEach(n2, new Action1<Node<LSubscription>>() {

            @Override
            public void call(Node<LSubscription> n) {
                ss.add(n.item);
            }

        });

        assertEquals(2, ss.size());
        assertEquals(2, ss.get(0).n);
        assertEquals(3, ss.get(1).n);
    }

    private Action1<Node<LSubscription>> newCounterAction(final AtomicInteger c) {
        return new Action1<Node<LSubscription>>() {

            @Override
            public void call(Node<LSubscription> t1) {
                c.incrementAndGet();
            }

        };
    }

    private static final PrintNode PRINT_NODE = new PrintNode();

    public static class PrintNode implements Action1<Node<Subscription>> {

        @Override
        public void call(Node<Subscription> n) {
            System.out.println("Node=>" + n.get());
        }

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

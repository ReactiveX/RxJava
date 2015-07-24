package rx.internal.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SynchronizedQueueTest {
    
    @Test
    public void testEquals() {
         SynchronizedQueue<Object> q = new SynchronizedQueue<Object>();
         assertTrue(q.equals(q));
    }

}

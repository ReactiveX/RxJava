/**
 * 
 */
package rx.internal.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author karnokd, 2014 dec. 2
 *
 */
public class SyncArrayQueueTest {
	@Test
	public void testSimpleOfferPoll() {
		SyncArrayQueue saq = new SyncArrayQueue();
		for (int i = 0; i < 10000; i++) {
			saq.offer(i);
			assertEquals(i, saq.poll());
		}
	}
	@Test
	public void testTriggerOneGrowth() {
		SyncArrayQueue saq = new SyncArrayQueue();
		for (int i = 0; i < 16; i++) {
			saq.offer(i);
		}
		for (int i = 0; i < 16; i++) {
			assertEquals(i, saq.poll());
		}		
	}
	@Test
	public void testTriggerGrowthHalfwayReading() {
		SyncArrayQueue saq = new SyncArrayQueue();
		for (int i = 0; i < 4; i++) {
			saq.offer(i);
		}
		for (int i = 0; i < 4; i++) {
			assertEquals(i, saq.poll());
		}
		for (int i = 4; i < 16; i++) {
			saq.offer(i);
		}
		for (int i = 4; i < 16; i++) {
			assertEquals(i, saq.poll());
		}
	}
}

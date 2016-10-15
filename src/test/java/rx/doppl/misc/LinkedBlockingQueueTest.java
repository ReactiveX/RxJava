package rx.doppl.misc;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by kgalligan on 10/15/16.
 */

    public class LinkedBlockingQueueTest
    {
        @Test
        public void testLBQ()
        {
            LinkedBlockingQueue<String> events = new LinkedBlockingQueue<String>();

            for(int i=0; i<1000; i++)
            {
                events.add("Heyo: "+ i);
            }
        }
    }

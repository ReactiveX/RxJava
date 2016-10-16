package rx.doppl.misc;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
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
//            List<String> events = new LinkedList<>();

            for(int i=0; i<100000; i++)
            {
                events.add("Heyo: "+ i);
            }

            for(String event : events)
            {
                System.out.println("before: "+ event);
            }
        }
    }

/**
 * Copyright 2016 Netflix, Inc.
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

import org.junit.Test;
import static org.junit.Assert.*;

public class TimeXTest {

    @Test
    public void timestamped() {
        Timestamped<Integer> ts1 = new Timestamped<Integer>(1L, 1);
        Timestamped<Integer> ts2 = new Timestamped<Integer>(1L, 1);
        Timestamped<Integer> ts3 = new Timestamped<Integer>(3L, 1);
        Timestamped<Integer> ts4 = new Timestamped<Integer>(3L, 2);
        Timestamped<Integer> ts5 = new Timestamped<Integer>(4L, null);
        Timestamped<Integer> ts6 = new Timestamped<Integer>(4L, null);
        Timestamped<Integer> ts7 = new Timestamped<Integer>(1L, new Integer(1)); // have unique reference
        Timestamped<Integer> ts8 = new Timestamped<Integer>(1L, 3);
        Timestamped<Integer> ts9 = new Timestamped<Integer>(1L, null);

        assertEquals(1L, ts1.getTimestampMillis());
        assertEquals(1, ts1.getValue().intValue());

        assertNotEquals(ts1, null);
        assertNotEquals(ts1, "string");

        assertEquals(ts1, ts1);
        assertEquals(ts1, ts2);
        assertEquals(ts1, ts7);
        assertEquals(ts7, ts1);

        assertNotEquals(ts1, ts3);
        assertNotEquals(ts1, ts4);
        assertNotEquals(ts1, ts8);
        assertNotEquals(ts8, ts1);
        assertNotEquals(ts1, ts9);
        assertNotEquals(ts9, ts1);

        assertNotEquals(ts1, ts5);
        assertNotEquals(ts5, ts1);

        assertEquals(ts5, ts6);

        assertEquals("Timestamped(timestampMillis = 1, value = 1)", ts1.toString());

        assertEquals(ts1.hashCode(), ts2.hashCode());

        assertEquals(ts5.hashCode(), ts6.hashCode());
    }

    @Test
    public void timeInterval() {
        TimeInterval<Integer> ts1 = new TimeInterval<Integer>(1L, 1);
        TimeInterval<Integer> ts2 = new TimeInterval<Integer>(1L, 1);
        TimeInterval<Integer> ts3 = new TimeInterval<Integer>(3L, 1);
        TimeInterval<Integer> ts4 = new TimeInterval<Integer>(3L, 2);
        TimeInterval<Integer> ts5 = new TimeInterval<Integer>(4L, null);
        TimeInterval<Integer> ts6 = new TimeInterval<Integer>(4L, null);
        TimeInterval<Integer> ts7 = new TimeInterval<Integer>(1L, new Integer(1)); // have unique reference
        TimeInterval<Integer> ts8 = new TimeInterval<Integer>(1L, 3);
        TimeInterval<Integer> ts9 = new TimeInterval<Integer>(1L, null);

        assertEquals(1L, ts1.getIntervalInMilliseconds());
        assertEquals(1, ts1.getValue().intValue());

        assertNotEquals(ts1, null);
        assertNotEquals(ts1, "string");

        assertEquals(ts1, ts1);
        assertEquals(ts1, ts2);
        assertEquals(ts1, ts7);
        assertEquals(ts7, ts1);

        assertNotEquals(ts1, ts3);
        assertNotEquals(ts1, ts4);
        assertNotEquals(ts1, ts8);
        assertNotEquals(ts8, ts1);
        assertNotEquals(ts1, ts9);
        assertNotEquals(ts9, ts1);

        assertNotEquals(ts1, ts5);
        assertNotEquals(ts5, ts1);

        assertEquals(ts5, ts6);

        assertEquals("TimeInterval [intervalInMilliseconds=1, value=1]", ts1.toString());

        assertEquals(ts1.hashCode(), ts2.hashCode());

        assertEquals(ts5.hashCode(), ts6.hashCode());
    }
}

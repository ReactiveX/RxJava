/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.schedulers;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import org.junit.Test;

public class TimedTest {

    @Test
    public void properties() {
        Timed<Integer> timed = new Timed<Integer>(1, 5, TimeUnit.SECONDS);

        assertEquals(1, timed.value().intValue());
        assertEquals(5, timed.time());
        assertEquals(5000, timed.time(TimeUnit.MILLISECONDS));
        assertSame(TimeUnit.SECONDS, timed.unit());
    }

    @Test
    public void hashCodeOf() {
        Timed<Integer> t1 = new Timed<Integer>(1, 5, TimeUnit.SECONDS);

        assertEquals(TimeUnit.SECONDS.hashCode() + 31 * (5 + 31 * 1), t1.hashCode());

        Timed<Integer> t2 = new Timed<Integer>(null, 5, TimeUnit.SECONDS);

        assertEquals(TimeUnit.SECONDS.hashCode() + 31 * (5 + 31 * 0), t2.hashCode());
    }

    @Test
    public void equalsWith() {
        Timed<Integer> t1 = new Timed<Integer>(1, 5, TimeUnit.SECONDS);
        Timed<Integer> t2 = new Timed<Integer>(1, 5, TimeUnit.SECONDS);
        Timed<Integer> t3 = new Timed<Integer>(2, 5, TimeUnit.SECONDS);
        Timed<Integer> t4 = new Timed<Integer>(1, 4, TimeUnit.SECONDS);
        Timed<Integer> t5 = new Timed<Integer>(1, 5, TimeUnit.MINUTES);

        assertEquals(t1, t1);
        assertEquals(t1, t2);

        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t2, t3);
        assertNotEquals(t2, t4);
        assertNotEquals(t2, t5);

        assertNotEquals(t3, t1);
        assertNotEquals(t3, t2);
        assertNotEquals(t3, t4);
        assertNotEquals(t3, t5);

        assertNotEquals(t4, t1);
        assertNotEquals(t4, t2);
        assertNotEquals(t4, t3);
        assertNotEquals(t4, t5);

        assertNotEquals(t5, t1);
        assertNotEquals(t5, t2);
        assertNotEquals(t5, t3);
        assertNotEquals(t5, t4);

        assertNotEquals(new Object(), t1);

        assertFalse(t1.equals(new Object()));
    }

    @Test
    public void toStringOf() {
        Timed<Integer> t1 = new Timed<Integer>(1, 5, TimeUnit.SECONDS);

        assertEquals("Timed[time=5, unit=SECONDS, value=1]", t1.toString());
    }

    @Test(expected = NullPointerException.class)
    public void timeUnitNullFail() throws Exception {
        new Timed<Integer>(1, 5, null);
    }
}

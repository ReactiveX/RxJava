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

package io.reactivex.internal.subscriptions;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.schedulers.Schedulers;

public class ArrayCompositeSubscriptionTest {

    @Test
    public void set() {
        ArrayCompositeSubscription ac = new ArrayCompositeSubscription(1);

        BooleanSubscription bs1 = new BooleanSubscription();

        ac.setResource(0, bs1);

        assertFalse(bs1.isCancelled());

        BooleanSubscription bs2 = new BooleanSubscription();

        ac.setResource(0, bs2);

        assertTrue(bs1.isCancelled());

        assertFalse(bs2.isCancelled());

        assertFalse(ac.isDisposed());

        ac.dispose();

        assertTrue(bs2.isCancelled());

        assertTrue(ac.isDisposed());

        BooleanSubscription bs3 = new BooleanSubscription();

        assertFalse(ac.setResource(0, bs3));

        assertTrue(bs3.isCancelled());

        assertFalse(ac.setResource(0, null));
    }

    @Test
    public void replace() {
        ArrayCompositeSubscription ac = new ArrayCompositeSubscription(1);

        BooleanSubscription bs1 = new BooleanSubscription();

        ac.replaceResource(0, bs1);

        assertFalse(bs1.isCancelled());

        BooleanSubscription bs2 = new BooleanSubscription();

        ac.replaceResource(0, bs2);

        assertFalse(bs1.isCancelled());

        assertFalse(bs2.isCancelled());

        assertFalse(ac.isDisposed());

        ac.dispose();

        assertTrue(bs2.isCancelled());

        assertTrue(ac.isDisposed());

        BooleanSubscription bs3 = new BooleanSubscription();

        ac.replaceResource(0, bs3);

        assertTrue(bs3.isCancelled());

        ac.replaceResource(0, null);
    }

    @Test
    public void disposeRace() {
        for (int i = 0; i < 500; i++) {
            final ArrayCompositeSubscription ac = new ArrayCompositeSubscription(1000);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ac.dispose();
                }
            };

            TestHelper.race(r, r, Schedulers.single());
        }
    }

    @Test
    public void setReplaceRace() {
        for (int i = 0; i < 500; i++) {
            final ArrayCompositeSubscription ac = new ArrayCompositeSubscription(1);

            final BooleanSubscription s1 = new BooleanSubscription();
            final BooleanSubscription s2 = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ac.setResource(0, s1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ac.replaceResource(0, s2);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

}

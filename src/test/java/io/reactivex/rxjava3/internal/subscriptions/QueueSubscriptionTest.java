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

package io.reactivex.rxjava3.internal.subscriptions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class QueueSubscriptionTest extends RxJavaTest {
    static final class EmptyQS extends BasicQueueSubscription<Integer> {

        private static final long serialVersionUID = -5312809687598840520L;

        @Override
        public int requestFusion(int mode) {
            return 0;
        }

        @Nullable
        @Override
        public Integer poll() throws Exception {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public void request(long n) {

        }

        @Override
        public void cancel() {

        }

    }

    static final class EmptyIntQS extends BasicIntQueueSubscription<Integer> {

        private static final long serialVersionUID = -1374033403007296252L;

        @Override
        public int requestFusion(int mode) {
            return 0;
        }

        @Nullable
        @Override
        public Integer poll() throws Exception {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public void request(long n) {

        }

        @Override
        public void cancel() {

        }

    }

    @Test
    public void noOfferBasic() {
        TestHelper.assertNoOffer(new EmptyQS());
    }

    @Test
    public void noOfferBasicInt() {
        TestHelper.assertNoOffer(new EmptyIntQS());
    }

    @Test
    public void empty() {
        TestHelper.checkEnum(EmptySubscription.class);

        assertEquals("EmptySubscription", EmptySubscription.INSTANCE.toString());

        TestHelper.assertNoOffer(EmptySubscription.INSTANCE);
    }
}

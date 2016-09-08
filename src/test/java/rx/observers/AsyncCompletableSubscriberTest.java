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
package rx.observers;

import java.util.*;

import org.junit.*;

import rx.Completable;
import rx.Observable;
import rx.exceptions.TestException;

public class AsyncCompletableSubscriberTest {

    static final class TestCS extends AsyncCompletableSubscriber {
        int started;

        int completions;

        final List<Throwable> errors = new ArrayList<Throwable>();

        @Override
        protected void onStart() {
            started++;
        }

        @Override
        public void onCompleted() {
            completions++;
            clear();
        }

        @Override
        public void onError(Throwable e) {
            errors.add(e);
            clear();
        }
    }

    @Test
    public void normal() {
        TestCS ts = new TestCS();

        Assert.assertFalse(ts.isUnsubscribed());

        Completable.complete().subscribe(ts);

        Assert.assertEquals(1, ts.started);
        Assert.assertEquals(1, ts.completions);
        Assert.assertEquals(ts.errors.toString(), 0, ts.errors.size());
        Assert.assertTrue(ts.isUnsubscribed());
    }

    @Test
    public void error() {
        TestCS ts = new TestCS();

        Assert.assertFalse(ts.isUnsubscribed());

        Completable.error(new TestException("Forced failure")).subscribe(ts);

        Assert.assertEquals(1, ts.started);
        Assert.assertEquals(0, ts.completions);
        Assert.assertEquals(ts.errors.toString(), 1, ts.errors.size());
        Assert.assertTrue(ts.errors.get(0).toString(), ts.errors.get(0) instanceof TestException);
        Assert.assertEquals("Forced failure", ts.errors.get(0).getMessage());
        Assert.assertTrue(ts.isUnsubscribed());
    }


    @Test
    public void unsubscribed() {
        TestCS ts = new TestCS();
        ts.unsubscribe();

        Assert.assertTrue(ts.isUnsubscribed());

        Observable.range(1, 10).toCompletable().subscribe(ts);

        Assert.assertEquals(0, ts.started);
        Assert.assertEquals(0, ts.completions);
        Assert.assertEquals(ts.errors.toString(), 0, ts.errors.size());
        Assert.assertTrue(ts.isUnsubscribed());
    }
}

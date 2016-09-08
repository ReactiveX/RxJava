/**
 * Copyright 2014 Netflix, Inc.
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
package rx.util;

import org.junit.*;

import rx.Observable;

public class AssertObservableTest {

    @Test
    public void testPassNotNull() {
        AssertObservable.assertObservableEqualsBlocking("foo", Observable.just(1, 2), Observable.just(1, 2));
    }

    @Test
    public void testPassNull() {
        AssertObservable.assertObservableEqualsBlocking("foo", null, null);
    }

    @Test
    public void testFailNotNull() {
        try {
            AssertObservable.assertObservableEqualsBlocking("foo", Observable.just(1, 2), Observable.just(1));
            Assert.fail("Failed to throw");
        } catch (AssertionError expected) {
            // expected
        }
    }

    @Test
    public void testFailNull() {
        try {
            AssertObservable.assertObservableEqualsBlocking("foo", Observable.just(1, 2), null);
            Assert.fail("Failed to throw");
        } catch (AssertionError expected) {
            // expected
        }
    }
}

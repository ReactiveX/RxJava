/**
 * Copyright 2015 Netflix, Inc.
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

package rx.singles;

import static org.junit.Assert.*;

import java.util.concurrent.Future;

import org.junit.Test;

import rx.Single;
import rx.exceptions.TestException;

/**
 * Test suite for {@link BlockingSingle}.
 */
public class BlockingSingleTest {

    @Test
    public void testSingleGet() {
        Single<String> single = Single.just("one");
        BlockingSingle<? extends String> blockingSingle = BlockingSingle.from(single);
        assertEquals("one", blockingSingle.value());
    }

    @Test
    public void testSingleError() {
        TestException expected = new TestException();
        Single<String> single = Single.error(expected);
        BlockingSingle<? extends String> blockingSingle = BlockingSingle.from(single);

        try {
            blockingSingle.value();
            fail("Expecting an exception to be thrown");
        } catch (Exception caughtException) {
            assertSame(expected, caughtException);
        }
    }

    @Test
    public void testSingleErrorChecked() {
        TestCheckedException expected = new TestCheckedException();
        Single<String> single = Single.error(expected);
        BlockingSingle<? extends String> blockingSingle = BlockingSingle.from(single);

        try {
            blockingSingle.value();
            fail("Expecting an exception to be thrown");
        } catch (Exception caughtException) {
            assertNotNull(caughtException.getCause());
            assertSame(expected, caughtException.getCause() );
        }
    }

    @Test
    public void testSingleToFuture() throws Exception {
        Single<String> single = Single.just("one");
        BlockingSingle<? extends String> blockingSingle = BlockingSingle.from(single);
        Future<? extends String> future = blockingSingle.toFuture();
        String result = future.get();
        assertEquals("one", result);
    }

    private static final class TestCheckedException extends Exception {

        /** */
        private static final long serialVersionUID = -5601856891331290034L;
    }
}

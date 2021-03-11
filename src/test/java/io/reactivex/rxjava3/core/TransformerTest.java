/*
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

package io.reactivex.rxjava3.core;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.ConverterTest.*;
import io.reactivex.rxjava3.exceptions.TestException;

public class TransformerTest extends RxJavaTest {

    @Test
    public void flowableTransformerThrows() {
        try {
            Flowable.just(1).compose((FlowableTransformer<Integer, Integer>) v -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void observableTransformerThrows() {
        try {
            Observable.just(1).compose((ObservableTransformer<Integer, Integer>) v -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void singleTransformerThrows() {
        try {
            Single.just(1).compose((SingleTransformer<Integer, Integer>) v -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void maybeTransformerThrows() {
        try {
            Maybe.just(1).compose((MaybeTransformer<Integer, Integer>) v -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void completableTransformerThrows() {
        try {
            Completable.complete().compose(v -> {
                throw new TestException("Forced failure");
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    // Test demos for signature generics in compose() methods. Just needs to compile.

    @Test
    public void observableGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Observable.just(a).compose(TransformerTest.testObservableTransformerCreator());
    }

    @Test
    public void singleGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Single.just(a).compose(TransformerTest.testSingleTransformerCreator());
    }

    @Test
    public void maybeGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Maybe.just(a).compose(TransformerTest.testMaybeTransformerCreator());
    }

    @Test
    public void flowableGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() { };

        Flowable.just(a).compose(TransformerTest.testFlowableTransformerCreator());
    }

    private static <T> ObservableTransformer<A<T, ?>, B<T>> testObservableTransformerCreator() {
        return a -> Observable.empty();
    }

    private static <T> SingleTransformer<A<T, ?>, B<T>> testSingleTransformerCreator() {
        return a -> Single.never();
    }

    private static <T> MaybeTransformer<A<T, ?>, B<T>> testMaybeTransformerCreator() {
        return a -> Maybe.empty();
    }

    private static <T> FlowableTransformer<A<T, ?>, B<T>> testFlowableTransformerCreator() {
        return a -> Flowable.empty();
    }
}

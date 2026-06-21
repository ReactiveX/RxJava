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

package io.reactivex.rxjava4.core;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava4.core.ConverterTest.*;
import io.reactivex.rxjava4.exceptions.TestException;

public class TransformerTest extends RxJavaTest {

    @Test
    public void flowableTransformerThrows() {
        try {
            Flowable.just(1).compose(_ -> {
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
            Observable.just(1).compose(_ -> {
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
            Single.just(1).compose(_ -> {
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
            Maybe.just(1).compose(_ -> {
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
            Completable.complete().compose(_ -> {
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
        A<String, Integer> a = new A<String, Integer>() /* NFI */ { };

        Observable.just(a).compose(TransformerTest.<String>testObservableTransformerCreator());
    }

    @Test
    public void singleGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() /* NFI */ { };

        Single.just(a).compose(TransformerTest.<String>testSingleTransformerCreator());
    }

    @Test
    public void maybeGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() /* NFI */ { };

        Maybe.just(a).compose(TransformerTest.<String>testMaybeTransformerCreator());
    }

    @Test
    public void flowableGenericsSignatureTest() {
        A<String, Integer> a = new A<String, Integer>() /* NFI */ { };

        Flowable.just(a).compose(TransformerTest.<String>testFlowableTransformerCreator());
    }

    private static <T> ObservableTransformer<A<T, ?>, B<T>> testObservableTransformerCreator() {
        return _ -> Observable.empty();
    }

    private static <T> SingleTransformer<A<T, ?>, B<T>> testSingleTransformerCreator() {
        return _ -> Single.never();
    }

    private static <T> MaybeTransformer<A<T, ?>, B<T>> testMaybeTransformerCreator() {
        return _ -> Maybe.empty();
    }

    private static <T> FlowableTransformer<A<T, ?>, B<T>> testFlowableTransformerCreator() {
        return _ -> Flowable.empty();
    }
}

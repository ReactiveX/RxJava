/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.exceptions.TestException;

public class TransformerTest {

    @Test
    public void flowableTransformerThrows() {
        try {
            Flowable.just(1).compose(new FlowableTransformer<Integer, Integer>() {
                @Override
                public Publisher<Integer> apply(Flowable<Integer> v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void observableTransformerThrows() {
        try {
            Observable.just(1).compose(new ObservableTransformer<Integer, Integer>() {
                @Override
                public Observable<Integer> apply(Observable<Integer> v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void singleTransformerThrows() {
        try {
            Single.just(1).compose(new SingleTransformer<Integer, Integer>() {
                @Override
                public Single<Integer> apply(Single<Integer> v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void maybeTransformerThrows() {
        try {
            Maybe.just(1).compose(new MaybeTransformer<Integer, Integer>() {
                @Override
                public Maybe<Integer> apply(Maybe<Integer> v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void completabeTransformerThrows() {
        try {
            Completable.complete().compose(new CompletableTransformer() {
                @Override
                public Completable apply(Completable v) {
                    throw new TestException("Forced failure");
                }
            });
            fail("Should have thrown!");
        } catch (TestException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }
}

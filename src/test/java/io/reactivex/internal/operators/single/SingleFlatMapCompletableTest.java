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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;

public class SingleFlatMapCompletableTest {

    @Test
    public void normal() {
        final boolean[] b = { false };
        
        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return Completable.complete().doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        b[0] = true;
                    }
                });
            }
        })
        .test()
        .assertResult();
        
        assertTrue(b[0]);
    }

    @Test
    public void error() {
        final boolean[] b = { false };
        
        Single.<Integer>error(new TestException())
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return Completable.complete().doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        b[0] = true;
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);
        
        assertFalse(b[0]);
    }

    @Test
    public void mapperThrows() {
        final boolean[] b = { false };
        
        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
        
        assertFalse(b[0]);
    }

    @Test
    public void mapperReturnsNull() {
        final boolean[] b = { false };
        
        Single.just(1)
        .flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer t) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
        
        assertFalse(b[0]);
    }

}

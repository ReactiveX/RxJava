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

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;

public class SingleDoOnTest {

    @Test
    public void doOnCancel() {
        final int[] count = { 0 };
        
        Single.never().doOnCancel(new Action() {
            @Override
            public void run() throws Exception { 
                count[0]++; 
            }
        }).test(true);
        
        assertEquals(1, count[0]);
    }

    @Test
    public void doOnError() {
        final Object[] event = { null };
        
        Single.error(new TestException()).doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                event[0] = e;
            }
        })
        .test();
        
        assertTrue(event[0].toString(), event[0] instanceof TestException);
    }

    @Test
    public void doOnSubscribe() {
        final int[] count = { 0 };
        
        Single.never().doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) throws Exception { 
                count[0]++; 
            }
        }).test();
        
        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSuccess() {
        final Object[] event = { null };
        
        Single.just(1).doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                event[0] = e;
            }
        })
        .test();
        
        assertEquals(1, event[0]);
    }

}

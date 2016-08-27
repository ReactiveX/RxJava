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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

public class CompletableTimeoutTest {

    @Test
    public void timeoutException() throws Exception {
        
        Completable.never()
        .timeout(100, TimeUnit.MILLISECONDS, Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void timeoutContinueOther() throws Exception {
        
        final int[] call = { 0 };
        
        Completable other = Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        });
        
        Completable.never()
        .timeout(100, TimeUnit.MILLISECONDS, Schedulers.io(), other)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
        
        assertEquals(1, call[0]);
    }

}

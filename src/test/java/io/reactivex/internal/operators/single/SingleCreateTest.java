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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;

public class SingleCreateTest {

    @Test(expected = NullPointerException.class)
    public void nullArgument() {
        Single.create(null);
    }
    
    @Test
    public void basic() {
        final Disposable d = Disposables.empty();
        
        Single.<Integer>create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> e) throws Exception {
                e.setDisposable(d);
                
                e.onSuccess(1);
                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
            }
        })
        .test()
        .assertResult(1);
        
        assertTrue(d.isDisposed());
    }
    
    @Test
    public void basicWithError() {
        final Disposable d = Disposables.empty();
        
        Single.<Integer>create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> e) throws Exception {
                e.setDisposable(d);
                
                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
        
        assertTrue(d.isDisposed());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void unsafeCreate() {
        Single.unsafeCreate(Single.just(1));
    }
}

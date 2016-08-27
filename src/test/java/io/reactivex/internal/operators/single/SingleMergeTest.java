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

import org.junit.Test;

import io.reactivex.Single;

public class SingleMergeTest {
    
    @Test
    public void mergeSingleSingle() {
        
        Single.merge(Single.just(Single.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void merge2() {
        Single.merge(Single.just(1), Single.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void merge3() {
        Single.merge(Single.just(1), Single.just(2), Single.just(3))
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void merge4() {
        Single.merge(Single.just(1), Single.just(2), Single.just(3), Single.just(4))
        .test()
        .assertResult(1, 2, 3, 4);
    }

}

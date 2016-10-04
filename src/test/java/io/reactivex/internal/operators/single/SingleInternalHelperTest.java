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

import io.reactivex.TestHelper;

public class SingleInternalHelperTest {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(SingleInternalHelper.class);
    }

    @Test
    public void noSuchElementCallableEnum() {
        assertEquals(1, SingleInternalHelper.NoSuchElementCallable.values().length);
        assertNotNull(SingleInternalHelper.NoSuchElementCallable.valueOf("INSTANCE"));
    }

    @Test
    public void toFlowableEnum() {
        assertEquals(1, SingleInternalHelper.ToFlowable.values().length);
        assertNotNull(SingleInternalHelper.ToFlowable.valueOf("INSTANCE"));
    }

    @Test
    public void toObservableEnum() {
        assertEquals(1, SingleInternalHelper.ToObservable.values().length);
        assertNotNull(SingleInternalHelper.ToObservable.valueOf("INSTANCE"));
    }
}

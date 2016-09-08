/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.util;

import org.junit.Test;
import static org.junit.Assert.*;

import rx.TestUtil;

public class UtilityFunctionsTest {
    @Test
    public void constructorShouldBePrivate() {
        TestUtil.checkUtilityClass(UtilityFunctions.class);
    }

    @Test
    public void alwaysFalse() {
        assertEquals(1, UtilityFunctions.AlwaysFalse.values().length);
        assertNotNull(UtilityFunctions.AlwaysFalse.valueOf("INSTANCE"));
        assertFalse(UtilityFunctions.alwaysFalse().call(1));
    }

    @Test
    public void alwaysTrue() {
        assertEquals(1, UtilityFunctions.AlwaysTrue.values().length);
        assertNotNull(UtilityFunctions.AlwaysTrue.valueOf("INSTANCE"));
        assertTrue(UtilityFunctions.alwaysTrue().call(1));
    }

}

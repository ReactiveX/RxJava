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

package io.reactivex.single;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.Single;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;

public class SingleSubscribeTest {

    @Test
    public void consumer() {
        final Integer[] value = { null };
        
        Single.just(1).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                value[0] = v;
            }
        });
        
        assertEquals((Integer)1, value[0]);
    }

    @Test
    public void biconsumer() {
        final Object[] value = { null, null };
        
        Single.just(1).subscribe(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                value[0] = v;
                value[1] = e;
            }
        });
        
        assertEquals((Integer)1, value[0]);
        assertNull(value[1]);
    }

    @Test
    public void biconsumerError() {
        final Object[] value = { null, null };
        
        TestException ex = new TestException();
        
        Single.error(ex).subscribe(new BiConsumer<Object, Throwable>() {
            @Override
            public void accept(Object v, Throwable e) throws Exception {
                value[0] = v;
                value[1] = e;
            }
        });
        
        assertNull(value[0]);
        assertEquals(ex, value[1]);
    }

}

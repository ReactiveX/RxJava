/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.jmh;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Exposes an Observable and Observer that increments n Integers and consumes them in a Blackhole.
 */
@State(Scope.Thread)
public class InputWithIncrementingIntegerTo1000000 extends InputWithIncrementingIntegerBase {
    @Param({ "1", "1000", "1000000" })
    public int size;

    @Override
    protected int getSize() {
        return size;
    }

}

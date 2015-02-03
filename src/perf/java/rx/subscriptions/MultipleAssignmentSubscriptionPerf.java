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

package rx.subscriptions;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.Subscription;

/**
 * Benchmark typical multiple-assignment subscription behavior.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*MultipleAssignmentSubscriptionPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*MultipleAssignmentSubscriptionPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MultipleAssignmentSubscriptionPerf {
    @State(Scope.Thread)
    public static class TheState {
        @Param({ "1", "1000", "100000" })
        public int loop;
        @Param({ "1", "5", "10", "100" })
        public int count;
        
        public final MultipleAssignmentSubscription csub = new MultipleAssignmentSubscription();
        
        public Subscription[] values;
        @Setup
        public void setup() {
            values = new Subscription[count];
            for (int i = 0; i < count; i++) {
                values[i] = new Subscription() {
                    @Override
                    public boolean isUnsubscribed() {
                        return false;
                    }
                    @Override
                    public void unsubscribe() {
                        
                    }
                };
            }
        }
    }
    @Benchmark
    public void add(TheState state) {
        MultipleAssignmentSubscription csub = state.csub;
        Subscription[] values = state.values;
        
        for (int i = state.loop; i > 0; i--) {
            for (int j = values.length - 1; j >= 0; j--) {
                csub.set(values[j]);
            }
        }
    }
    @Benchmark
    public void addLocal(TheState state, Blackhole bh) {
        MultipleAssignmentSubscription csub = new MultipleAssignmentSubscription();
        Subscription[] values = state.values;
        
        for (int i = state.loop; i > 0; i--) {
            for (int j = values.length - 1; j >= 0; j--) {
                csub.set(values[j]);
            }
        }
        bh.consume(csub);
    }
}

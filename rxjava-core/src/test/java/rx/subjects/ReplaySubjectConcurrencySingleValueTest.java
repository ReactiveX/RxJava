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
package rx.subjects;

import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;

public class ReplaySubjectConcurrencySingleValueTest extends ReplaySubjectConcurrencyTest {
    @Override
    public ReplaySubject createReplaySubject() {
        return ReplaySubject.create(1);
    }

    /**
     * https://github.com/Netflix/RxJava/issues/1147
     */
    @Test
    @Override
    public void testRaceForTerminalState() {
        final List<Integer> expected = Arrays.asList(1);
        for (int i = 0; i < 100000; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            Observable.just(1).subscribeOn(Schedulers.computation()).cache(1).subscribe(ts);
            ts.awaitTerminalEvent();
            ts.assertReceivedOnNext(expected);
            ts.assertTerminalEvent();
        }
    }
}

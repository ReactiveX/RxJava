/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.subjects.PublishSubject;

public class OperationParallelMergeTest {

    @Test
    public void testParallelMerge() {
        PublishSubject<String> p1 = PublishSubject.<String> create();
        PublishSubject<String> p2 = PublishSubject.<String> create();
        PublishSubject<String> p3 = PublishSubject.<String> create();
        PublishSubject<String> p4 = PublishSubject.<String> create();

        Observable<Observable<String>> fourStreams = Observable.<Observable<String>> from(p1, p2, p3, p4);

        Observable<Observable<String>> twoStreams = OperationParallelMerge.parallelMerge(fourStreams, 2);
        Observable<Observable<String>> threeStreams = OperationParallelMerge.parallelMerge(fourStreams, 3);

        List<? super Observable<String>> fourList = fourStreams.toList().toBlockingObservable().last();
        List<? super Observable<String>> threeList = threeStreams.toList().toBlockingObservable().last();
        List<? super Observable<String>> twoList = twoStreams.toList().toBlockingObservable().last();

        assertEquals(4, fourList.size());
        assertEquals(3, threeList.size());
        assertEquals(2, twoList.size());
    }
}

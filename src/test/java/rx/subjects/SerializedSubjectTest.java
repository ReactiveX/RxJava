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

import static org.junit.Assert.assertSame;

import java.util.Arrays;

import org.junit.Test;

import rx.observers.TestSubscriber;

public class SerializedSubjectTest {

    @Test
    public void testBasic() {
        SerializedSubject<String, String> subject = new SerializedSubject<String, String>(PublishSubject.<String> create());
        TestSubscriber<String> ts = new TestSubscriber<String>();
        subject.subscribe(ts);
        subject.onNext("hello");
        subject.onCompleted();
        ts.awaitTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList("hello"));
    }
    
    @Test
    public void testDontWrapSerializedSubjectAgain() {
        PublishSubject<Object> s = PublishSubject.create();
        Subject<Object, Object> s1 = s.toSerialized();
        Subject<Object, Object> s2 = s1.toSerialized();
        assertSame(s1, s2);
    }
}

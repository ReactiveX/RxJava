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
package rx;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;

import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class ThrottleWithTimeoutTests {

    @Test
    public void testThrottle() {
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        TestScheduler s = new TestScheduler();
        PublishSubject<Integer> o = PublishSubject.create();
        o.throttleWithTimeout(500, TimeUnit.MILLISECONDS, s).subscribe(observer);

        // send events with simulated time increments
        s.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        o.onNext(1); // skip
        o.onNext(2); // deliver
        s.advanceTimeTo(501, TimeUnit.MILLISECONDS);
        o.onNext(3); // skip
        s.advanceTimeTo(600, TimeUnit.MILLISECONDS);
        o.onNext(4); // skip
        s.advanceTimeTo(700, TimeUnit.MILLISECONDS);
        o.onNext(5); // skip
        o.onNext(6); // deliver at 1300 after 500ms has passed since onNext(5)
        s.advanceTimeTo(1300, TimeUnit.MILLISECONDS);
        o.onNext(7); // deliver
        s.advanceTimeTo(1800, TimeUnit.MILLISECONDS);
        o.onCompleted();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(2);
        inOrder.verify(observer).onNext(6);
        inOrder.verify(observer).onNext(7);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
}

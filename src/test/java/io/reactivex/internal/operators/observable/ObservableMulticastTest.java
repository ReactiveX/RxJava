/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.observable;

public class ObservableMulticastTest {
    // FIXME operator multicast not supported
//
//    @Test
//    public void testMulticast() {
//        Subject<String, String> source = PublishSubject.create();
//
//        ConnectableObservable<String> multicasted = new OperatorMulticast<String, String>(source, new PublishSubjectFactory());
//
//        Observer<String> observer = TestHelper.mockObserver(Observer.class);
//        multicasted.subscribe(observer);
//
//        source.onNext("one");
//        source.onNext("two");
//
//        multicasted.connect();
//
//        source.onNext("three");
//        source.onNext("four");
//        source.onComplete();
//
//        verify(observer, never()).onNext("one");
//        verify(observer, never()).onNext("two");
//        verify(observer, times(1)).onNext("three");
//        verify(observer, times(1)).onNext("four");
//        verify(observer, times(1)).onComplete();
//
//    }
//
//    @Test
//    public void testMulticastConnectTwice() {
//        Subject<String, String> source = PublishSubject.create();
//
//        ConnectableObservable<String> multicasted = new OperatorMulticast<String, String>(source, new PublishSubjectFactory());
//
//        @SuppressWarnings("unchecked")
//        Observer<String> observer = TestHelper.mockObserver(Observer.class);
//        multicasted.subscribe(observer);
//
//        source.onNext("one");
//
//        Subscription sub = multicasted.connect();
//        Subscription sub2 = multicasted.connect();
//
//        source.onNext("two");
//        source.onComplete();
//
//        verify(observer, never()).onNext("one");
//        verify(observer, times(1)).onNext("two");
//        verify(observer, times(1)).onComplete();
//
//        assertEquals(sub, sub2);
//
//    }
//
//    @Test
//    public void testMulticastDisconnect() {
//        Subject<String, String> source = PublisherSubject.create();
//
//        ConnectableObservable<String> multicasted = new OperatorMulticast<String, String>(source, new PublishSubjectFactory());
//
//        @SuppressWarnings("unchecked")
//        Observer<String> observer = mock(Observer.class);
//        multicasted.subscribe(observer);
//
//        source.onNext("one");
//
//        Subscription connection = multicasted.connect();
//        source.onNext("two");
//
//        connection.unsubscribe();
//        source.onNext("three");
//
//        // subscribe again
//        multicasted.subscribe(observer);
//        // reconnect
//        multicasted.connect();
//        source.onNext("four");
//        source.onComplete();
//
//        verify(observer, never()).onNext("one");
//        verify(observer, times(1)).onNext("two");
//        verify(observer, never()).onNext("three");
//        verify(observer, times(1)).onNext("four");
//        verify(observer, times(1)).onComplete();
//
//    }
//
//    private static final class PublishSubjectFactory implements Func0<Subject<String, String>> {
//
//        @Override
//        public Subject<String, String> call() {
//            return PublisherSubject.<String> create();
//        }
//
//    }
}

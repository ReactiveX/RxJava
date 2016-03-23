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

package io.reactivex.internal.operators.observable;

public class NbpOperatorMulticastTest {
    // FIXME operator multicast not supported
//
//    @Test
//    public void testMulticast() {
//        Subject<String, String> source = NbpPublishSubject.create();
//
//        ConnectableObservable<String> multicasted = new OperatorMulticast<String, String>(source, new PublishSubjectFactory());
//
//        @SuppressWarnings("unchecked")
//        NbpObserver<String> NbpObserver = mock(NbpObserver.class);
//        multicasted.subscribe(NbpObserver);
//
//        source.onNext("one");
//        source.onNext("two");
//
//        multicasted.connect();
//
//        source.onNext("three");
//        source.onNext("four");
//        source.onCompleted();
//
//        verify(NbpObserver, never()).onNext("one");
//        verify(NbpObserver, never()).onNext("two");
//        verify(NbpObserver, times(1)).onNext("three");
//        verify(NbpObserver, times(1)).onNext("four");
//        verify(NbpObserver, times(1)).onCompleted();
//
//    }
//
//    @Test
//    public void testMulticastConnectTwice() {
//        Subject<String, String> source = NbpPublishSubject.create();
//
//        ConnectableObservable<String> multicasted = new OperatorMulticast<String, String>(source, new PublishSubjectFactory());
//
//        @SuppressWarnings("unchecked")
//        NbpObserver<String> NbpObserver = mock(NbpObserver.class);
//        multicasted.subscribe(NbpObserver);
//
//        source.onNext("one");
//
//        Subscription sub = multicasted.connect();
//        Subscription sub2 = multicasted.connect();
//        
//        source.onNext("two");
//        source.onCompleted();
//
//        verify(NbpObserver, never()).onNext("one");
//        verify(NbpObserver, times(1)).onNext("two");
//        verify(NbpObserver, times(1)).onCompleted();
//        
//        assertEquals(sub, sub2);
//
//    }
//
//    @Test
//    public void testMulticastDisconnect() {
//        Subject<String, String> source = NbpPublishSubject.create();
//
//        ConnectableObservable<String> multicasted = new OperatorMulticast<String, String>(source, new PublishSubjectFactory());
//
//        @SuppressWarnings("unchecked")
//        NbpObserver<String> NbpObserver = mock(NbpObserver.class);
//        multicasted.subscribe(NbpObserver);
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
//        multicasted.subscribe(NbpObserver);
//        // reconnect
//        multicasted.connect();
//        source.onNext("four");
//        source.onCompleted();
//
//        verify(NbpObserver, never()).onNext("one");
//        verify(NbpObserver, times(1)).onNext("two");
//        verify(NbpObserver, never()).onNext("three");
//        verify(NbpObserver, times(1)).onNext("four");
//        verify(NbpObserver, times(1)).onCompleted();
//
//    }
//    
//    private static final class PublishSubjectFactory implements Func0<Subject<String, String>> {
//
//        @Override
//        public Subject<String, String> call() {
//            return NbpPublishSubject.<String> create();
//        }
//        
//    }
}
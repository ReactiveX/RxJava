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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.subjects.PublishSubject;

public class ObservableSkipUntilTest {
    Observer<Object> NbpObserver;

    @Before
    public void before() {
        NbpObserver = TestHelper.mockObserver();
    }

    @Test
    public void normal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(other);
        m.subscribe(NbpObserver);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, times(1)).onNext(3);
        verify(NbpObserver, times(1)).onNext(4);
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void otherNeverFires() {
        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(Observable.never());

        m.subscribe(NbpObserver);

        source.onNext(0);
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onNext(any());
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void otherEmpty() {
        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(Observable.empty());

        m.subscribe(NbpObserver);

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onNext(any());
        verify(NbpObserver, never()).onComplete();
    }

    @Test
    public void otherFiresAndCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(other);
        m.subscribe(NbpObserver);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);
        other.onComplete();

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, times(1)).onNext(3);
        verify(NbpObserver, times(1)).onNext(4);
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void sourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(other);
        m.subscribe(NbpObserver);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);
        other.onComplete();

        source.onNext(2);
        source.onError(new RuntimeException("Forced failure"));

        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, times(1)).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
    }

    @Test
    public void otherThrowsImmediately() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(other);
        m.subscribe(NbpObserver);

        source.onNext(0);
        source.onNext(1);

        other.onError(new RuntimeException("Forced failure"));

        verify(NbpObserver, never()).onNext(any());
        verify(NbpObserver, times(1)).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
    }
}
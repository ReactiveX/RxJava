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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.operators.observable.BlockingObservableIterable.BlockingObservableIterator;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class BlockingObservableToIteratorTest extends RxJavaTest {

    @Test
    public void toIterator() {
        Observable<String> obs = Observable.just("one", "two", "three");

        Iterator<String> it = obs.blockingIterable().iterator();

        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        assertTrue(it.hasNext());
        assertEquals("two", it.next());

        assertTrue(it.hasNext());
        assertEquals("three", it.next());

        assertFalse(it.hasNext());

    }

    @Test(expected = TestException.class)
    public void toIteratorWithException() {
        Observable<String> obs = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onNext("one");
                observer.onError(new TestException());
            }
        });

        Iterator<String> it = obs.blockingIterable().iterator();

        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        assertTrue(it.hasNext());
        it.next();
    }

    @Test
    public void dispose() {
        BlockingObservableIterator<Integer> it = new BlockingObservableIterator<>(128);

        assertFalse(it.isDisposed());

        it.dispose();

        assertTrue(it.isDisposed());
    }

    @Test
    public void interruptWait() {
        BlockingObservableIterator<Integer> it = new BlockingObservableIterator<>(128);

        try {
            Thread.currentThread().interrupt();

            it.hasNext();
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyThrowsNoSuch() {
        BlockingObservableIterator<Integer> it = new BlockingObservableIterator<>(128);
        it.onComplete();
        it.next();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void remove() {
        BlockingObservableIterator<Integer> it = new BlockingObservableIterator<>(128);
        it.remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void disposedIteratorHasNextReturns() {
        Iterator<Integer> it = PublishSubject.<Integer>create()
                .blockingIterable().iterator();
        ((Disposable)it).dispose();
        assertFalse(it.hasNext());
        it.next();
    }

    @Test
    public void asyncDisposeUnblocks() {
        final Iterator<Integer> it = PublishSubject.<Integer>create()
                .blockingIterable().iterator();

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                ((Disposable)it).dispose();
            }
        }, 1, TimeUnit.SECONDS);

        assertFalse(it.hasNext());
    }
}

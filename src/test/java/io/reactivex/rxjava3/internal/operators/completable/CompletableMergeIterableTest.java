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

package io.reactivex.rxjava3.internal.operators.completable;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableMergeIterableTest extends RxJavaTest {

    @Test
    public void errorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestObserver<Void> to = Completable.merge(
                        Arrays.asList(ps1.ignoreElements(), ps2.ignoreElements())).test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex);
                    }
                };

                TestHelper.race(r1, r2);

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void cancelAfterHasNext() {
        final TestObserver<Void> to = new TestObserver<>();

        Completable.merge(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return new Iterator<Completable>() {
                    @Override
                    public boolean hasNext() {
                        to.dispose();
                        return true;
                    }

                    @Override
                    public Completable next() {
                        return Completable.complete();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }).subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void cancelAfterNext() {
        final TestObserver<Void> to = new TestObserver<>();

        Completable.merge(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return new Iterator<Completable>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Completable next() {
                        to.dispose();
                        return Completable.complete();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }).subscribe(to);

        to.assertEmpty();
    }
}

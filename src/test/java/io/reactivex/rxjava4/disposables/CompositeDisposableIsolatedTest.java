/*
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

package io.reactivex.rxjava4.disposables;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class CompositeDisposableIsolatedTest extends RxJavaTest {

    @Test
    public void disposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            Runnable run = cd::dispose;

            TestHelper.race(run, run);
        }
    }

    @Test
    public void addRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            Runnable run = () -> cd.add(Disposable.empty());

            TestHelper.race(run, run);
        }
    }

    @Test
    public void addAllRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            Runnable run = () -> cd.addAll(Disposable.empty());

            TestHelper.race(run, run);
        }
    }

    @Test
    public void removeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            final Disposable d1 = Disposable.empty();

            cd.add(d1);

            Runnable run = () -> cd.remove(d1);

            TestHelper.race(run, run);
        }
    }

    @Test
    public void deleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            final Disposable d1 = Disposable.empty();

            cd.add(d1);

            Runnable run = () -> cd.delete(d1);

            TestHelper.race(run, run);
        }
    }

    @Test
    public void clearRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            final Disposable d1 = Disposable.empty();

            cd.add(d1);

            Runnable run = cd::clear;

            TestHelper.race(run, run);
        }
    }

    @Test
    public void addDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            Runnable run = cd::dispose;

            Runnable run2 = () -> cd.add(Disposable.empty());

            TestHelper.race(run, run2);
        }
    }

    @Test
    public void addAllDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            Runnable run = cd::dispose;

            Runnable run2 = () -> cd.addAll(Disposable.empty());

            TestHelper.race(run, run2);
        }
    }

    @Test
    public void removeDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            final Disposable d1 = Disposable.empty();

            cd.add(d1);

            Runnable run = cd::dispose;

            Runnable run2 = () -> cd.remove(d1);

            TestHelper.race(run, run2);
        }
    }

    @Test
    public void deleteDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            final Disposable d1 = Disposable.empty();

            cd.add(d1);

            Runnable run = cd::dispose;

            Runnable run2 = () -> cd.delete(d1);

            TestHelper.race(run, run2);
        }
    }

    @Test
    public void clearDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            final Disposable d1 = Disposable.empty();

            cd.add(d1);

            Runnable run = cd::dispose;

            Runnable run2 = cd::clear;

            TestHelper.race(run, run2);
        }
    }

    @Test
    public void sizeDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final CompositeDisposable cd = new CompositeDisposable();

            final Disposable d1 = Disposable.empty();

            cd.add(d1);

            Runnable run = cd::dispose;

            Runnable run2 = cd::size;

            TestHelper.race(run, run2);
        }
    }
}

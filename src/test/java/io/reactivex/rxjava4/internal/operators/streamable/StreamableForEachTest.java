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

package io.reactivex.rxjava4.internal.operators.streamable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.exceptions.*;

@Isolated
public class StreamableForEachTest extends StreamableBaseTest {

    @Test
    public void forEachCheckedCrash() {
        var ex = assertThrows(ThrowableWrapper.class, () -> {
            Streamable.just(1)
            .forEach(_ -> {
                throw new Exception("test");
            })
            .await()
            ;
        });

        assertEquals("test", ex.getCause().getMessage());
    }

    @Test
    public void forEachUncheckedCrash() {
        var ex = assertThrows(TestException.class, () -> {
            Streamable.just(1)
            .forEach(_ -> {
                throw new TestException("test");
            })
            .await()
            ;
        });

        assertEquals("test", ex.getMessage());
    }

    @Test
    public void forEachExecCheckedCrash() throws Throwable {
        withCachedExecutor(exec -> {
            var ex = assertThrows(ThrowableWrapper.class, () -> {
                Streamable.just(1)
                .forEach(_ -> {
                    throw new Exception("test");
                }, exec)
                .await()
                ;
            });

            assertEquals("test", ex.getCause().getMessage());
        });
    }

    @Test
    public void forEachExecUncheckedCrash() throws Throwable {
        withCachedExecutor(exec -> {
            var ex = assertThrows(TestException.class, () -> {
                Streamable.just(1)
                .forEach(_ -> {
                    throw new TestException("test");
                }, exec)
                .await()
                ;
            });

            assertEquals("test", ex.getMessage());
        });
    }

    @Test
    public void forEachBiCheckedCrash() throws Throwable {
        withVirtual(exec -> {
            var ex = assertThrows(ThrowableWrapper.class, () -> {
                Streamable.just(1)
                .forEach((_, _) -> {
                    throw new Exception("test");
                }, new CompositeDisposable(), exec)
                .await()
                ;
            });

            assertEquals("test", ex.getCause().getMessage());
        });
    }

    @Test
    public void forEachBiUncheckedCrash() throws Throwable {
        withVirtual(exec -> {
            var ex = assertThrows(TestException.class, () -> {
                Streamable.just(1)
                .forEach((_, _) -> {
                    throw new TestException("test");
                }, new CompositeDisposable(), exec)
                .await()
                ;
            });

            assertEquals("test", ex.getMessage());
        });
    }

    @Test
    public void forEachBiUncheckedPropagation() throws Throwable {
        withVirtual(exec -> {
            var ex = assertThrows(TestException.class, () -> {
                Streamable.error(new TestException("test"))
                .forEach((_, _) -> {
                }, new CompositeDisposable(), exec)
                .await()
                ;
            });

            assertEquals("test", ex.getMessage());
        });
    }

    @Test
    public void forEachOutsideCancel() {
        var cd = new CompositeDisposable();
        var counter = new AtomicInteger();

        assertThrows(CancellationException.class, () -> {
            Streamable.range(1, 5)
            .forEach(_ -> {
                counter.getAndIncrement();
                cd.dispose();
                Thread.sleep(10); // The body may fall off faster than the cancel can propagate out, so sleep
            }, cd)
            .await();
        });

        assertTrue(cd.isDisposed(), "cd was not disposed");
        assertEquals(1, counter.get());
    }

    @Test
    public void forEachBiOutsideCancel() throws Throwable {
        withVirtual(exec -> {
            var cd = new CompositeDisposable();
            var counter = new AtomicInteger();

            assertThrows(CancellationException.class, () -> {
                Streamable.range(1, 5)
                .forEach((_, _) -> {
                    counter.getAndIncrement();
                    cd.dispose();
                    Thread.sleep(10); // The body may fall off faster than the cancel can propagate out, so sleep
                }, cd, exec)
                .await();
            });

            assertTrue(cd.isDisposed(), "cd was not disposed");
            assertEquals(1, counter.get());
        });
    }

    @Test
    public void forEachBiInsideCancel() throws Throwable {
        withVirtual(exec -> {
            var cd = new CompositeDisposable();
            var counter = new AtomicInteger();

            Streamable.range(1, 5)
            .forEach((_, s) -> {
                counter.getAndIncrement();
                s.dispose();
            }, cd, exec)
            .await();

            assertFalse(cd.isDisposed(), "cd was disposed");
            assertEquals(1, counter.get());
        });
    }
}

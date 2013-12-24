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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observer;

public class SafeObserverTest {

    @Test
    public void onNextFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        try {
            OBSERVER_ONNEXT_FAIL(onError).onNext("one");
            fail("expects exception to be thrown");
        } catch (Exception e) {
            // expected
            assertNull(onError.get());
        }
    }

    @Test
    public void onNextFailureSafe() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        try {
            new SafeObserver<String>(OBSERVER_ONNEXT_FAIL(onError)).onNext("one");
            assertNotNull(onError.get());
        } catch (Exception e) {
            fail("expects exception to be passed to onError");
        }
    }

    @Test
    public void onCompletedFailure() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        try {
            OBSERVER_ONCOMPLETED_FAIL(onError).onCompleted();
            fail("expects exception to be thrown");
        } catch (Exception e) {
            // expected
            assertNull(onError.get());
        }
    }

    @Test
    public void onCompletedFailureSafe() {
        AtomicReference<Throwable> onError = new AtomicReference<Throwable>();
        try {
            new SafeObserver<String>(OBSERVER_ONCOMPLETED_FAIL(onError)).onCompleted();
            assertNotNull(onError.get());
        } catch (Exception e) {
            fail("expects exception to be passed to onError");
        }
    }

    @Test
    public void onErrorFailure() {
        try {
            OBSERVER_ONERROR_FAIL().onError(new RuntimeException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void onErrorFailureSafe() {
        try {
            new SafeObserver<String>(OBSERVER_ONERROR_FAIL()).onError(new RuntimeException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            // expected since onError fails so SafeObserver can't help
        }
    }

    @Test
    public void onNextOnErrorFailure() {
        try {
            OBSERVER_ONNEXT_ONERROR_FAIL().onError(new RuntimeException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void onNextOnErrorFailureSafe() {
        try {
            new SafeObserver<String>(OBSERVER_ONNEXT_ONERROR_FAIL()).onError(new RuntimeException("error!"));
            fail("expects exception to be thrown");
        } catch (Exception e) {
            // expected since onError fails so SafeObserver can't help
        }
    }

    private static Observer<String> OBSERVER_ONNEXT_FAIL(final AtomicReference<Throwable> onError) {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                onError.set(e);
            }

            @Override
            public void onNext(String args) {
                throw new RuntimeException("onNextFail");
            }
        };

    }

    private static Observer<String> OBSERVER_ONNEXT_ONERROR_FAIL() {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException("onErrortFail");
            }

            @Override
            public void onNext(String args) {
                throw new RuntimeException("onNextFail");
            }

        };
    }

    private static Observer<String> OBSERVER_ONERROR_FAIL() {
        return new Observer<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException("onErrorFail");
            }

            @Override
            public void onNext(String args) {

            }

        };
    }

    private static Observer<String> OBSERVER_ONCOMPLETED_FAIL(final AtomicReference<Throwable> onError) {
        return new Observer<String>() {

            @Override
            public void onCompleted() {
                throw new RuntimeException("onCompletedFail");
            }

            @Override
            public void onError(Throwable e) {
                onError.set(e);
            }

            @Override
            public void onNext(String args) {

            }

        };
    }
}

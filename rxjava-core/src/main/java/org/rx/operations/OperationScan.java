package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.rx.functions.Func2;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IObserver;

/* package */class OperationScan {
    /**
     * Applies an accumulator function over an observable sequence and returns each intermediate result with the specified source and accumulator.
     * 
     * @param sequence
     *            An observable sequence of elements to project.
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator function to be invoked on each element from the sequence.
     * 
     * @return An observable sequence whose elements are the result of accumulating the output from the list of IObservables.
     * @see http://msdn.microsoft.com/en-us/library/hh211665(v=vs.103).aspx
     */
    public static <T> IObservable<T> scan(IObservable<T> sequence, T initialValue, Func2<T, T, T> accumulator) {
        return new Accumulator<T>(sequence, initialValue, accumulator);
    }

    /**
     * Applies an accumulator function over an observable sequence and returns each intermediate result with the specified source and accumulator.
     * 
     * @param sequence
     *            An observable sequence of elements to project.
     * @param accumulator
     *            An accumulator function to be invoked on each element from the sequence.
     * 
     * @return An observable sequence whose elements are the result of accumulating the output from the list of IObservables.
     * @see http://msdn.microsoft.com/en-us/library/hh211665(v=vs.103).aspx
     */
    public static <T> IObservable<T> scan(IObservable<T> sequence, Func2<T, T, T> accumulator) {
        return new Accumulator<T>(sequence, null, accumulator);
    }

    private static class Accumulator<T> extends AbstractIObservable<T> {
        private final IObservable<T> sequence;
        private final T initialValue;
        private Func2<T, T, T> accumlatorFunction;

        private Accumulator(IObservable<T> sequence, T initialValue, Func2<T, T, T> accumulator) {
            this.sequence = sequence;
            this.initialValue = initialValue;
            this.accumlatorFunction = accumulator;
        }

        public IDisposable subscribe(final IObserver<T> observer) {

            final AtomicWatchableSubscription s = new AtomicWatchableSubscription();
            final AtomicWatcher<T> watcher = new AtomicWatcher<T>(observer, s);

            s.setActual(sequence.subscribe(new IObserver<T>() {
                private T acc = initialValue;
                private boolean hasSentInitialValue = false;

                /**
                 * We must synchronize this because we can't allow
                 * multiple threads to execute the 'accumulatorFunction' at the same time because
                 * the accumulator code very often will be doing mutation of the 'acc' object such as a non-threadsafe HashMap
                 * 
                 * Because it's synchronized it's using non-atomic variables since everything in this method is single-threaded
                 */
                public synchronized void onNext(T value) {
                    if (acc == null) {
                        // we assume that acc is not allowed to be returned from accumulatorValue
                        // so it's okay to check null as being the state we initialize on
                        acc = value;
                        // this is all we do for this first value if we didn't have an initialValue
                        return;
                    }
                    if (!hasSentInitialValue) {
                        hasSentInitialValue = true;
                        watcher.onNext(acc);
                    }

                    try {

                        acc = accumlatorFunction.call(acc, value);
                        if (acc == null) {
                            onError(new IllegalArgumentException("Null is an unsupported return value for an accumulator."));
                            return;
                        }
                        watcher.onNext(acc);
                    } catch (Exception ex) {
                        watcher.onError(ex);
                        // unsubscribe since we blew up
                        s.unsubscribe();
                    }
                }

                public void onError(Exception ex) {
                    watcher.onError(ex);
                }

                // synchronized because we access 'hasSentInitialValue'
                public synchronized void onCompleted() {
                    // if only one sequence value existed, we send it without any accumulation
                    if (!hasSentInitialValue) {
                        watcher.onNext(acc);
                    }
                    watcher.onCompleted();
                }
            }));

            return s;
        }
    }

    public static class UnitTest {

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testScanIntegersWithInitialValue() {
            @SuppressWarnings("unchecked")
            IObserver<Integer> watcher = mock(IObserver.class);

            IObservable<Integer> observable = WatchableExtensions.toWatchable(1, 2, 3);

            IObservable<Integer> m = scan(observable, 0, new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer t1, Integer t2) {
                    return t1 + t2;
                }

            });
            m.subscribe(watcher);

            verify(watcher, never()).onError(any(Exception.class));
            verify(watcher, times(1)).onNext(0);
            verify(watcher, times(1)).onNext(1);
            verify(watcher, times(1)).onNext(3);
            verify(watcher, times(1)).onNext(6);
            verify(watcher, times(4)).onNext(anyInt());
            verify(watcher, times(1)).onCompleted();
            verify(watcher, never()).onError(any(Exception.class));
        }

        @Test
        public void testScanIntegersWithoutInitialValue() {
            @SuppressWarnings("unchecked")
            IObserver<Integer> watcher = mock(IObserver.class);

            IObservable<Integer> observable = WatchableExtensions.toWatchable(1, 2, 3);

            IObservable<Integer> m = scan(observable, new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer t1, Integer t2) {
                    return t1 + t2;
                }

            });
            m.subscribe(watcher);

            verify(watcher, never()).onError(any(Exception.class));
            verify(watcher, never()).onNext(0);
            verify(watcher, times(1)).onNext(1);
            verify(watcher, times(1)).onNext(3);
            verify(watcher, times(1)).onNext(6);
            verify(watcher, times(3)).onNext(anyInt());
            verify(watcher, times(1)).onCompleted();
            verify(watcher, never()).onError(any(Exception.class));
        }

        @Test
        public void testScanIntegersWithoutInitialValueAndOnlyOneValue() {
            @SuppressWarnings("unchecked")
            IObserver<Integer> watcher = mock(IObserver.class);

            IObservable<Integer> observable = WatchableExtensions.toWatchable(1);

            IObservable<Integer> m = scan(observable, new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer t1, Integer t2) {
                    return t1 + t2;
                }

            });
            m.subscribe(watcher);

            verify(watcher, never()).onError(any(Exception.class));
            verify(watcher, never()).onNext(0);
            verify(watcher, times(1)).onNext(1);
            verify(watcher, times(1)).onNext(anyInt());
            verify(watcher, times(1)).onCompleted();
            verify(watcher, never()).onError(any(Exception.class));
        }
    }

}

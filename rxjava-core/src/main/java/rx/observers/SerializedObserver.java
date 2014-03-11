package rx.observers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observer;

public class SerializedObserver<T> implements Observer<T> {

    private final AtomicReference<State> state = new AtomicReference<State>(State.createNew());
    private final Observer<? super T> s;

    public SerializedObserver(Observer<? super T> s) {
        this.s = s;
    }

    final AtomicInteger received = new AtomicInteger();
    final AtomicInteger counter = new AtomicInteger();
    final AtomicInteger offered = new AtomicInteger();
    static AtomicInteger decremented = new AtomicInteger();

    @Override
    public void onCompleted() {
        State current = null;
        State newState = null;
        do {
            current = state.get();
            if (current.isTerminated()) {
                // already received terminal state
                return;
            }
            newState = current.complete();
        } while (!state.compareAndSet(current, newState));
        terminateIfNecessary(newState);
    }

    @Override
    public void onError(Throwable e) {
        State current = null;
        State newState = null;
        do {
            current = state.get();
            if (current.isTerminated()) {
                // already received terminal state
                return;
            }
            newState = current.error(e);
        } while (!state.compareAndSet(current, newState));
        terminateIfNecessary(newState);
    }

    AtomicInteger conc = new AtomicInteger();
    AtomicInteger lost = new AtomicInteger();
    Set<Object> items = Collections.synchronizedSet(new HashSet<Object>());

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(T t) {
        State current = null;
        State newState = null;

        int contention = 0;
        State orig = null;
        do {
            current = state.get();
            if (orig == null) {
                orig = current;
            }
            if (current.isTerminated()) {
                // already received terminal state
                return;
            }
            newState = current.offerItem(t);
            contention++;
        } while (!state.compareAndSet(current, newState));

        do {
            current = state.get();
            newState = current.startProcessing();
        } while (!state.compareAndSet(current, newState));
        if (newState.shouldProcess()) {
            // drain queue 
            Object[] items = newState.queue;
            for (int i = 0; i < items.length; i++) {
                s.onNext((T) items[i]);
                counter.incrementAndGet();
            }

            // finish processing to let this thread move on
            do {
                current = state.get();
                newState = current.finishProcessing(items.length);
            } while (!state.compareAndSet(current, newState));

        }
        terminateIfNecessary(newState);
    }

    @SuppressWarnings("unchecked")
    private void terminateIfNecessary(State current) {
        if (current.isTerminated()) {
            State newState = null;
            do {
                current = state.get();
                newState = current.startTermination();
            } while (!state.compareAndSet(current, newState));

            if (newState.shouldProcess()) {
                // drain any items left
                for (int i = 0; i < newState.queue.length; i++) {
                    s.onNext((T) newState.queue[i]);
                }

                // now terminate
                if (newState.onComplete) {
                    s.onCompleted();
                } else {
                    s.onError(newState.onError);
                }
            }
        }
    }

    public static class State {
        final boolean shouldProcess;
        final boolean isSomeoneProcessing;
        final int queueSize;
        final Object[] queue;
        final boolean onComplete;
        final Throwable onError;

        private final static Object[] EMPTY = new Object[0];
        private final static Object[] PROCESS_SELF = new Object[1];

        private final static State NON_TERMINATED_EMPTY = new State(false, false, 0, false, null, EMPTY);

        public State(boolean shouldProcess, boolean isSomeoneProcessing, int queueSize, boolean onComplete, Throwable onError, Object[] queue) {
            this.shouldProcess = shouldProcess;
            this.isSomeoneProcessing = isSomeoneProcessing;
            this.queueSize = queueSize;
            this.queue = queue;
            this.onComplete = onComplete;
            this.onError = onError;
        }

        public static State createNew() {
            return new State(false, false, 0, false, null, EMPTY);
        }

        public boolean shouldProcess() {
            return shouldProcess;
        }

        public boolean isTerminated() {
            return onComplete || onError != null;
        }

        public State complete() {
            return new State(false, isSomeoneProcessing, queueSize, true, onError, queue);
        }

        public State error(Throwable e) {
            // immediately empty the queue and emit error as soon as possible
            return new State(false, isSomeoneProcessing, 0, onComplete, e, EMPTY);
        }

        public State startTermination() {
            if (isSomeoneProcessing) {
                return new State(false, isSomeoneProcessing, queueSize, onComplete, onError, queue);
            } else {
                return new State(true, true, queueSize, onComplete, onError, queue);
            }
        }

        public State offerItem(Object item) {
            if (isTerminated()) {
                // return count of 0 meaning don't emit as we are terminated
                return new State(false, isSomeoneProcessing, 0, onComplete, onError, EMPTY);
            } else {
                int idx = queue.length;
                Object[] newQueue = new Object[idx + 1];
                System.arraycopy(queue, 0, newQueue, 0, idx);
                newQueue[idx] = item;

                // we just add to queue
                return new State(false, isSomeoneProcessing, queueSize + 1, onComplete, onError, newQueue);
            }
        }

        public State startProcessing() {
            if (isSomeoneProcessing) {
                return new State(false, true, queueSize, onComplete, onError, queue);
            } else {
                return new State(true, true, queueSize, onComplete, onError, queue);
            }
        }

        public State finishProcessing(int numOnNextSent) {
            int size = queueSize - numOnNextSent;
            if (size > 0 || isTerminated()) {
                // if size == 0 but we are terminated then it's an empty queue
                Object[] newQueue = EMPTY;
                if (size > 0) {
                    newQueue = new Object[queue.length - numOnNextSent];
                    System.arraycopy(queue, numOnNextSent, newQueue, 0, newQueue.length);
                }
                return new State(false, false, size, onComplete, onError, newQueue);
            } else {
                return NON_TERMINATED_EMPTY;
            }
        }

        @Override
        public String toString() {
            return "State => shouldProcess: " + shouldProcess + " processing: " + isSomeoneProcessing + " queueSize: " + queueSize + " queue: " + queue.length + " terminated: " + isTerminated();
        }

    }
}

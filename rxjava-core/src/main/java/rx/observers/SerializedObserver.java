package rx.observers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observer;

public class SerializedObserver<T> implements Observer<T> {

    private final AtomicReference<State> state = new AtomicReference<State>(State.createNew());
    private final Observer<T> s;

    public SerializedObserver(Observer<T> s) {
        this.s = s;
    }

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
        if (newState.count == 0) {
            s.onCompleted();
        }
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
        if (newState.count == 0) {
            s.onError(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(T t) {
        State current = null;
        State newState = null;
        do {
            current = state.get();
            if (current.isTerminated()) {
                // already received terminal state
                return;
            }
            newState = current.increment(t);
        } while (!state.compareAndSet(current, newState));

        if (newState.count == 1) {
            // this thread wins and will emit then drain queue if it concurrently gets added to
            try {
                s.onNext(t);
            } finally {
                // decrement after finishing
                do {
                    current = state.get();
                    newState = current.decrement();
                } while (!state.compareAndSet(current, newState));
            }

            // drain queue if exists
            // we do "if" instead of "while" so we don't starve one thread
            if (newState.queue.length > 0) {
                Object[] items = newState.queue;
                for (int i = 0; i < items.length; i++) {
                    s.onNext((T) items[i]);
                }
                // clear state of queue
                do {
                    current = state.get();
                    newState = current.drain(items.length);
                } while (!state.compareAndSet(current, newState));
                terminateIfNecessary(newState);
            } else {
                terminateIfNecessary(newState);
            }

        }
    }

    private void terminateIfNecessary(State state) {
        if (state.isTerminated()) {
            if (state.onComplete) {
                s.onCompleted();
            } else {
                s.onError(state.onError);
            }
        }
    }

    public static class State {
        final int count;
        final Object[] queue;
        final boolean onComplete;
        final Throwable onError;

        private final static Object[] EMPTY = new Object[0];

        private final static State NON_TERMINATED_EMPTY = new State(0, false, null, EMPTY);
        private final static State NON_TERMINATED_SINGLE = new State(1, false, null, EMPTY);

        public State(int count, boolean onComplete, Throwable onError, Object[] queue) {
            this.count = count;
            this.queue = queue;
            this.onComplete = onComplete;
            this.onError = onError;
        }

        public static State createNew() {
            return new State(0, false, null, EMPTY);
        }

        public boolean isTerminated() {
            return onComplete || onError != null;
        }

        public State complete() {
            return new State(count, true, onError, queue);
        }

        public State error(Throwable e) {
            return new State(count, onComplete, e, queue);
        }

        AtomicInteger max = new AtomicInteger();

        public State increment(Object item) {
            if (count == 0) {
                // no concurrent requests so don't queue, we'll process immediately
                if (isTerminated()) {
                    // return count of 0 meaning don't emit as we are terminated
                    return new State(0, onComplete, onError, EMPTY);
                } else {
                    return NON_TERMINATED_SINGLE;
                }
            } else {
                // concurrent requests so need to queue
                int idx = queue.length;
                Object[] newQueue = new Object[idx + 1];
                System.arraycopy(queue, 0, newQueue, 0, idx);
                newQueue[idx] = item;

                if (max.get() < newQueue.length) {
                    max.set(newQueue.length);
                    System.out.println("max queue: " + newQueue.length);
                }

                return new State(count + 1, onComplete, onError, newQueue);
            }
        }

        public State decrement() {
            if (count > 1 || isTerminated()) {
                return new State(count - 1, onComplete, onError, queue);
            } else {
                return NON_TERMINATED_EMPTY;
            }
        }

        public State drain(int c) {
            Object[] newQueue = new Object[queue.length - c];
            System.arraycopy(queue, c, newQueue, 0, newQueue.length);
            return new State(count - c, onComplete, onError, newQueue);
        }

    }
}

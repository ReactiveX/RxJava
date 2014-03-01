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
        System.out.println("********** onCompleted");
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
        System.out.println("********** onError");
        terminateIfNecessary(newState);
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
            newState = current.offerItem(t);
        } while (!state.compareAndSet(current, newState));

        if (newState.shouldProcess()) {
            // this thread wins and will emit then drain queue if it concurrently gets added to
            s.onNext(t);

            // drain queue if exists
            // we do "if" instead of "while" so we don't starve one thread
            Object[] items = newState.queue;
            for (int i = 0; i < items.length; i++) {
                s.onNext((T) items[i]);
            }

            // finish processing to let this thread move on
            do {
                current = state.get();
                newState = current.finishProcessing(items.length + 1); // the + 1 is for the first onNext of itself
            } while (!state.compareAndSet(current, newState));
            System.out.println("********** finishProcessing");
            terminateIfNecessary(newState);
        }
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

        private final static State NON_TERMINATED_EMPTY = new State(false, false, 0, false, null, EMPTY);
        private final static State NON_TERMINATED_PROCESS_SELF = new State(true, true, 1, false, null, EMPTY);

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
            return new State(false, isSomeoneProcessing, queueSize, onComplete, e, EMPTY);
        }

        public State startTermination() {
            if (isSomeoneProcessing) {
                System.out.println("start terminate and DO NOT process => queue size: " + (queueSize + 1));
                return new State(false, isSomeoneProcessing, queueSize, onComplete, onError, queue);
            } else {
                System.out.println("start terminate and process => queue size: " + (queueSize + 1));
                return new State(true, isSomeoneProcessing, queueSize, onComplete, onError, queue);
            }
        }

        AtomicInteger max = new AtomicInteger();

        public State offerItem(Object item) {
            if (queueSize == 0) {
                // no concurrent requests so don't queue, we'll process immediately
                if (isTerminated()) {
                    // return count of 0 meaning don't emit as we are terminated
                    return new State(false, false, 0, onComplete, onError, EMPTY);
                } else {
                    return NON_TERMINATED_PROCESS_SELF;
                }
            } else {
                // there are items queued so we need to queue
                int idx = queue.length;
                Object[] newQueue = new Object[idx + 1];
                System.arraycopy(queue, 0, newQueue, 0, idx);
                newQueue[idx] = item;

                if (isSomeoneProcessing) {
                    // we just add to queue
                    return new State(false, isSomeoneProcessing, queueSize + 1, onComplete, onError, newQueue);
                } else {
                    // we add to queue and claim work
                    return new State(false, true, queueSize + 1, onComplete, onError, newQueue);
                }
            }
        }

        public State finishProcessing(int numOnNextSent) {
            int numOnNextFromQueue = numOnNextSent - 1; // we remove the "self" onNext as it doesn't affect the queue
            int size = queueSize - numOnNextFromQueue;
            System.out.println("finishProcessing => queue size: " + size + "   after processing: " + numOnNextSent);
            if (size > 1 || isTerminated()) {
                Object[] newQueue = new Object[queue.length - numOnNextFromQueue];
                System.arraycopy(queue, numOnNextFromQueue, newQueue, 0, newQueue.length);
                return new State(false, false, size, onComplete, onError, newQueue);
            } else {
                return NON_TERMINATED_EMPTY;
            }
        }

    }
}

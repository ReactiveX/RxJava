package rx.observers;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observer;

/* package */class SerializedObserverViaStateMachine<T> implements Observer<T> {

    private final AtomicReference<State> state = new AtomicReference<State>(State.createNew());
    private final Observer<? super T> s;

    public SerializedObserverViaStateMachine(Observer<? super T> s) {
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

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(T t) {
        State current = null;
        State newState = null;

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
        } while (!state.compareAndSet(current, newState));

        if (newState.shouldProcess()) {
            int numItemsProcessed = 0;
            try {
                if (newState == State.PROCESS_SELF) {
                    s.onNext(t);
                    numItemsProcessed++;
                } else {
                    // drain queue 
                    Object[] items = newState.queue;
                    for (int i = 0; i < items.length; i++) {
                        s.onNext((T) items[i]);
                        numItemsProcessed++;
                    }
                }
            } finally {
                // finish processing to let this thread move on
                do {
                    current = state.get();
                    newState = current.finishProcessing(numItemsProcessed);
                } while (!state.compareAndSet(current, newState));
            }
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
        private final static Object[] PROCESS_SELF_QUEUE = new Object[1];

        private final static State NON_TERMINATED_EMPTY = new State(false, false, 0, false, null, EMPTY);
        private final static State PROCESS_SELF = new State(true, true, 1, false, null, PROCESS_SELF_QUEUE);

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
            if (queueSize == 0) {
                if (isTerminated()) {
                    // return count of 0 meaning don't emit as we are terminated
                    return new State(false, isSomeoneProcessing, 0, onComplete, onError, EMPTY);
                } else {
                    // no concurrent requests so don't queue, we'll process immediately
                    return PROCESS_SELF;
                }
            } else {
                // there are items queued so we need to queue
                int idx = queue.length;
                Object[] newQueue = new Object[idx + 1];
                System.arraycopy(queue, 0, newQueue, 0, idx);
                newQueue[idx] = item;

                if (isSomeoneProcessing) {
                    // we just add to queue
                    return new State(false, true, queueSize + 1, onComplete, onError, newQueue);
                } else {
                    // we add to queue and claim work
                    return new State(true, true, queueSize + 1, onComplete, onError, newQueue);
                }
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

package rx.observers;

import java.util.ArrayList;

import rx.Observer;
import rx.operators.NotificationLite;

/**
 * Enforce single-threaded, serialized, ordered execution of onNext, onCompleted, onError.
 * <p>
 * When multiple threads are notifying they will be serialized by:
 * <p>
 * <li>Allowing only one thread at a time to emit</li>
 * <li>Adding notifications to a queue if another thread is already emitting</li>
 * <li>Not holding any locks or blocking any threads while emitting</li>
 * <p>
 * 
 * @param <T>
 */
public class SerializedObserver<T> implements Observer<T> {
    private final Observer<? super T> actual;

    private boolean emitting = false;
    private boolean terminated = false;
    private ArrayList<Object> queue = new ArrayList<Object>();
    private NotificationLite<T> on = NotificationLite.instance();

    public SerializedObserver(Observer<? super T> s) {
        this.actual = s;
    }

    @Override
    public void onCompleted() {
        boolean canEmit = false;
        ArrayList<Object> list = null;
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
            if (!emitting) {
                // emit immediately
                emitting = true;
                canEmit = true;
                if (queue.size() > 0) {
                    list = queue; // copy reference
                    queue = new ArrayList<Object>(); // new version;
                }
            } else {
                // someone else is already emitting so just queue it
                queue.add(on.completed());
            }
        }

        if (canEmit) {
            // we won the right to emit
            try {
                drainQueue(list);
                actual.onCompleted();
            } finally {
                synchronized (this) {
                    emitting = false;
                }
            }
        }
    }

    @Override
    public void onError(final Throwable e) {
        boolean canEmit = false;
        ArrayList<Object> list = null;
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
            if (!emitting) {
                // emit immediately
                emitting = true;
                canEmit = true;
                if (queue.size() > 0) {
                    list = queue; // copy reference
                    queue = new ArrayList<Object>(); // new version;
                }
            } else {
                // someone else is already emitting so just queue it ... after eliminating the queue to shortcut
                queue.clear();
                queue.add(on.error(e));
            }
        }
        if (canEmit) {
            // we won the right to emit
            try {
                drainQueue(list);
                actual.onError(e);
            } finally {
                synchronized (this) {
                    emitting = false;
                }
            }
        }
    }

    @Override
    public void onNext(T t) {
        boolean canEmit = false;
        ArrayList<Object> list = null;
        synchronized (this) {
            if (terminated) {
                return;
            }
            if (!emitting) {
                // emit immediately
                emitting = true;
                canEmit = true;
                if (queue.size() > 0) {
                    list = queue; // copy reference
                    queue = new ArrayList<Object>(); // new version;
                }
            } else {
                // someone else is already emitting so just queue it
                queue.add(on.next(t));
            }
        }
        if (canEmit) {
            // we won the right to emit
            try {
                drainQueue(list);
                actual.onNext(t);
            } finally {
                synchronized (this) {
                    if (terminated) {
                        list = queue; // copy reference
                        queue = new ArrayList<Object>(); // new version;
                    } else {
                        // release this thread
                        emitting = false;
                        canEmit = false;
                    }
                }
            }
        }

        // if terminated this will still be true so let's drain the rest of the queue
        if (canEmit) {
            drainQueue(list);
        }
    }

    public void drainQueue(ArrayList<Object> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        for (Object v : list) {
            on.accept(actual, v);
        }
    }
}

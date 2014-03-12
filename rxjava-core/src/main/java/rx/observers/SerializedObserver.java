package rx.observers;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observer;

public class SerializedObserver<T> implements Observer<T> {
    private final Observer<? super T> actual;
    private final AtomicInteger count = new AtomicInteger();
    private final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<Object>();

    private static Sentinel NULL_SENTINEL = new Sentinel();
    private static Sentinel COMPLETE_SENTINEL = new Sentinel();

    private static class Sentinel {

    }

    private static class ErrorSentinel extends Sentinel {
        final Throwable e;

        ErrorSentinel(Throwable e) {
            this.e = e;
        }
    }

    public SerializedObserver(Observer<? super T> s) {
        this.actual = s;
    }

    @Override
    public void onCompleted() {
        queue.add(COMPLETE_SENTINEL);
        doIt();
    }

    @Override
    public void onError(final Throwable e) {
        queue.add(new ErrorSentinel(e));
        doIt();
    }

    @Override
    public void onNext(T t) {
        queue.add(t);
        doIt();
    }

    public void doIt() {
        if (count.getAndIncrement() == 0) {
            do {
                Object v = queue.poll();
                if (v != null) {
                    if (v instanceof Sentinel) {
                        if (v == NULL_SENTINEL) {
                            actual.onNext(null);
                        } else if (v == COMPLETE_SENTINEL) {
                            actual.onCompleted();
                        } else if (v instanceof ErrorSentinel) {
                            actual.onError(((ErrorSentinel) v).e);
                        }
                    } else {
                        actual.onNext((T) v);
                    }
                }
            } while (count.decrementAndGet() > 0);
        }
    }
}

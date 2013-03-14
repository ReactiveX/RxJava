package rx.observables;

import rx.Observer;
import rx.Scheduler;
import rx.util.functions.Action0;

public class ScheduledObserver<T> implements Observer<T> {
    private final Observer<T> underlying;
    private final Scheduler scheduler;

    public ScheduledObserver(Observer<T> underlying, Scheduler scheduler) {
        this.underlying = underlying;
        this.scheduler = scheduler;
    }

    @Override
    public void onCompleted() {
        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                underlying.onCompleted();
            }
        });
    }

    @Override
    public void onError(Exception e) {
        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                underlying.onCompleted();
            }
        });
    }

    @Override
    public void onNext(T args) {
        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                underlying.onCompleted();
            }
        });
    }
}

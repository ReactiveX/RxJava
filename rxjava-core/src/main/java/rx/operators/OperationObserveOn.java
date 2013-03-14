package rx.operators;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

public class OperationObserveOn {

    public static <T> Func1<Observer<T>, Subscription> observeOn(Observable<T> source, Scheduler scheduler) {
        return new ObserveOn<T>(source, scheduler);
    }

    private static class ObserveOn<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<T> source;
        private final Scheduler scheduler;

        public ObserveOn(Observable<T> source, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            return source.subscribe(new ScheduledObserver<T>(observer, scheduler));
        }
    }

    private static class ScheduledObserver<T> implements Observer<T> {
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
}

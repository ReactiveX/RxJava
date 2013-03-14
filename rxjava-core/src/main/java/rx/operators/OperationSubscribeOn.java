package rx.operators;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

public class OperationSubscribeOn {

    public static <T> Func1<Observer<T>, Subscription> subscribeOn(Observable<T> source, Scheduler scheduler) {
        return new SubscribeOn<T>(source, scheduler);
    }

    private static class SubscribeOn<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<T> source;
        private final Scheduler scheduler;

        public SubscribeOn(Observable<T> source, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            return scheduler.schedule(new Func0<Subscription>() {
                @Override
                public Subscription call() {
                    return new ScheduledSubscription(source.subscribe(observer), scheduler);
                }
            });
        }
    }

    private static class ScheduledSubscription implements Subscription {
        private final Subscription underlying;
        private final Scheduler scheduler;

        private ScheduledSubscription(Subscription underlying, Scheduler scheduler) {
            this.underlying = underlying;
            this.scheduler = scheduler;
        }

        @Override
        public void unsubscribe() {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    underlying.unsubscribe();
                }
            });
        }
    }
}

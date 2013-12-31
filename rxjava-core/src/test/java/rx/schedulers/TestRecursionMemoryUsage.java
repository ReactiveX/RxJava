package rx.schedulers;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func2;

/**
 * Used for manual testing of memory leaks with recursive schedulers.
 * 
 */
public class TestRecursionMemoryUsage {

    public static void main(String args[]) {
        usingFunc2(Schedulers.newThread());
        usingAction0(Schedulers.newThread());

//        usingFunc2(Schedulers.currentThread());
//        usingAction0(Schedulers.currentThread());

        usingFunc2(Schedulers.threadPoolForComputation());
        usingAction0(Schedulers.threadPoolForComputation());
    }

    protected static void usingFunc2(final Scheduler scheduler) {
        System.out.println("************ usingFunc2: " + scheduler);
        Observable.create(new OnSubscribeFunc<Long>() {

            @Override
            public Subscription onSubscribe(final Observer<? super Long> o) {
                return scheduler.schedule(0L, new Func2<Scheduler, Long, Subscription>() {

                    @Override
                    public Subscription call(Scheduler innerScheduler, Long i) {
                        i++;
                        if (i % 500000 == 0) {
                            System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                            o.onNext(i);
                        }
                        if (i == 100000000L) {
                            o.onCompleted();
                            return Subscriptions.empty();
                        }

                        return innerScheduler.schedule(i, this);
                    }
                });
            }
        }).toBlockingObservable().last();
    }

    protected static void usingAction0(final Scheduler scheduler) {
        System.out.println("************ usingAction0: " + scheduler);
        Observable.create(new OnSubscribeFunc<Long>() {

            @Override
            public Subscription onSubscribe(final Observer<? super Long> o) {
                return scheduler.schedule(new Action1<Action0>() {

                    private long i = 0;

                    @Override
                    public void call(Action0 self) {
                        i++;
                        if (i % 500000 == 0) {
                            System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                            o.onNext(i);
                        }
                        if (i == 100000000L) {
                            o.onCompleted();
                            return;
                        }
                        self.call();
                    }
                });
            }
        }).toBlockingObservable().last();
    }
}

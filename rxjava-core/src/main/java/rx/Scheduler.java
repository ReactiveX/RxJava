package rx;

import rx.util.functions.Action0;
import rx.util.functions.Func0;

import java.util.concurrent.TimeUnit;

public interface Scheduler {

    Subscription schedule(Action0 action);

    Subscription schedule(Func0<Subscription> action);

    Subscription schedule(Action0 action, long timespan, TimeUnit unit);

    Subscription schedule(Func0<Subscription> action, long timespan, TimeUnit unit);

    long now();

}

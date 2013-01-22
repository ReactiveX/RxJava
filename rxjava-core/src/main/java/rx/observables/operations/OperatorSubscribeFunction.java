package rx.observables.operations;

import rx.observables.Observable;
import rx.observables.Observer;
import rx.observables.Subscription;
import rx.util.AtomicObserver;
import rx.util.functions.Func1;

/**
 * A "marker" interface used for internal operators implementing the "subscribe" function and turned into Observables using {@link Observable#create(Func1)}.
 * <p>
 * This marker is used by it to treat these implementations as "trusted".
 * <p>
 * NOTE: If you use this interface you are declaring that your implementation:
 * <ul>
 * <li>is thread-safe</li>
 * <li>doesn't need additional wrapping by {@link AtomicObserver}</li>
 * <li>obeys the contract of onNext, onError, onComplete</li>
 * </ul>
 * 
 * @param <T>
 */
public interface OperatorSubscribeFunction<T> extends Func1<Observer<T>, Subscription> {

}

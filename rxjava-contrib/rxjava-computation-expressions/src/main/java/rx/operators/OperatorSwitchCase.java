package rx.operators;

import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func0;

/**
 * Select an observable from a map based on a case key returned by a selector
 * function when an observer subscribes.
 * 
 * @param <K>
 *            the case key type
 * @param <R>
 *            the result value type
 */
public final class OperatorSwitchCase<K, R> implements OnSubscribe<R> {
    final Func0<? extends K> caseSelector;
    final Map<? super K, ? extends Observable<? extends R>> mapOfCases;
    final Observable<? extends R> defaultCase;

    public OperatorSwitchCase(Func0<? extends K> caseSelector,
            Map<? super K, ? extends Observable<? extends R>> mapOfCases,
            Observable<? extends R> defaultCase) {
        this.caseSelector = caseSelector;
        this.mapOfCases = mapOfCases;
        this.defaultCase = defaultCase;
    }

    @Override
    public void call(Subscriber<? super R> t1) {
        Observable<? extends R> target;
        try {
            K caseKey = caseSelector.call();
            if (mapOfCases.containsKey(caseKey)) {
                target = mapOfCases.get(caseKey);
            } else {
                target = defaultCase;
            }
        } catch (Throwable t) {
            t1.onError(t);
            return;
        }
        target.subscribe(t1);
    }
}
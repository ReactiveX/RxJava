package rx;

import java.util.Map;

import rx.operators.OperationConditionals;
import rx.util.functions.Func0;

/**
 * Imperative statements expressed as Observable operators.
 */
public class Statement {

    /**
     * Return a particular one of several possible Observables based on a case
     * selector.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/switchCase.png">
     * 
     * @param <K>
     *            the case key type
     * @param <R>
     *            the result value type
     * @param caseSelector
     *            the function that produces a case key when an
     *            Observer subscribes
     * @param mapOfCases
     *            a map that maps a case key to an Observable
     * @return a particular Observable chosen by key from the map of
     *         Observables, or an empty Observable if no Observable matches the
     *         key
     */
    public static <K, R> Observable<R> switchCase(Func0<? extends K> caseSelector,
            Map<? super K, ? extends IObservable<? extends R>> mapOfCases) {
        return switchCase(caseSelector, mapOfCases, Observable.<R> empty());
    }

    /**
     * Return a particular one of several possible Observables based on a case
     * selector and run it on the designated scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/switchCase.s.png">
     * 
     * @param <K>
     *            the case key type
     * @param <R>
     *            the result value type
     * @param caseSelector
     *            the function that produces a case key when an
     *            Observer subscribes
     * @param mapOfCases
     *            a map that maps a case key to an Observable
     * @param scheduler
     *            the scheduler where the empty observable is observed
     * @return a particular Observable chosen by key from the map of
     *         Observables, or an empty Observable if no Observable matches the
     *         key, but one that runs on the designated scheduler in either case
     */
    public static <K, R> Observable<R> switchCase(Func0<? extends K> caseSelector,
            Map<? super K, ? extends IObservable<? extends R>> mapOfCases, Scheduler scheduler) {
        return switchCase(caseSelector, mapOfCases, Observable.<R> empty(scheduler));
    }

    /**
     * Return a particular one of several possible Observables based on a case
     * selector, or a default Observable if the case selector does not map to
     * a particular one.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/switchCase.png">
     * 
     * @param <K>
     *            the case key type
     * @param <R>
     *            the result value type
     * @param caseSelector
     *            the function that produces a case key when an
     *            Observer subscribes
     * @param mapOfCases
     *            a map that maps a case key to an Observable
     * @param defaultCase
     *            the default Observable if the {@code mapOfCases} doesn't contain a value for the key returned by the {@case caseSelector}
     * @return a particular Observable chosen by key from the map of
     *         Observables, or the default case if no Observable matches the key
     */
    public static <K, R> Observable<R> switchCase(Func0<? extends K> caseSelector,
            Map<? super K, ? extends IObservable<? extends R>> mapOfCases,
            IObservable<? extends R> defaultCase) {
        return Observable.create(OperationConditionals.switchCase(caseSelector, mapOfCases, defaultCase));
    }

    /**
     * Return an Observable that replays the emissions from the source
     * Observable, and then continues to replay them so long as a condtion is
     * true.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/doWhile.png">
     * 
     * @param postCondition
     *            the post condition to test after the source
     *            Observable completes
     * @return an Observable that replays the emissions from the source
     *         Observable, and then continues to replay them so long as the post
     *         condition is true
     */
    public static <T> Observable<T> doWhile(IObservable<T> source, Func0<Boolean> postCondition) {
        return Observable.create(OperationConditionals.doWhile(source, postCondition));
    }

    /**
     * Return an Observable that replays the emissions from the source
     * Observable so long as a condtion is true.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/whileDo.png">
     * 
     * @param preCondition
     *            the condition to evaluate before subscribing to or
     *            replaying the source Observable
     * @return an Observable that replays the emissions from the source
     *         Observable so long as <code>preCondition</code> is true
     */
    public static <T> Observable<T> whileDo(IObservable<T> source, Func0<Boolean> preCondition) {
        return Observable.create(OperationConditionals.whileDo(source, preCondition));
    }

    /**
     * Return an Observable that emits the emissions from a specified Observable
     * if a condition evaluates to true, otherwise return an empty Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/ifThen.png">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides whether to emit the emissions
     *            from the <code>then</code> Observable
     * @param then
     *            the Observable sequence to emit to if {@code condition} is {@code true}
     * @return an Observable that mimics the {@code then} Observable if the {@code condition} function evaluates to true, or an empty
     *         Observable otherwise
     */
    public static <R> Observable<R> ifThen(Func0<Boolean> condition, IObservable<? extends R> then) {
        return ifThen(condition, then, Observable.<R> empty());
    }

    /**
     * Return an Observable that emits the emissions from a specified Observable
     * if a condition evaluates to true, otherwise return an empty Observable
     * that runs on a specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/ifThen.s.png">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides whether to emit the emissions
     *            from the <code>then</code> Observable
     * @param then
     *            the Observable sequence to emit to if {@code condition} is {@code true}
     * @param scheduler
     *            the Scheduler on which the empty Observable runs if the
     *            in case the condition returns false
     * @return an Observable that mimics the {@code then} Observable if the {@code condition} function evaluates to true, or an empty
     *         Observable running on the specified Scheduler otherwise
     */
    public static <R> Observable<R> ifThen(Func0<Boolean> condition, IObservable<? extends R> then, Scheduler scheduler) {
        return ifThen(condition, then, Observable.<R> empty(scheduler));
    }

    /**
     * Return an Observable that emits the emissions from one specified
     * Observable if a condition evaluates to true, or from another specified
     * Observable otherwise.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/ifThen.e.png">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides which Observable to emit the
     *            emissions from
     * @param then
     *            the Observable sequence to emit to if {@code condition} is {@code true}
     * @param orElse
     *            the Observable sequence to emit to if {@code condition} is {@code false}
     * @return an Observable that mimics either the {@code then} or {@code orElse} Observables depending on a condition function
     */
    public static <R> Observable<R> ifThen(Func0<Boolean> condition, IObservable<? extends R> then,
            IObservable<? extends R> orElse) {
        return Observable.create(OperationConditionals.ifThen(condition, then, orElse));
    }

}

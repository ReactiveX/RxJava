package rx.observables;

import rx.Observable;
import rx.functions.Func1;
import rx.joins.Pattern2;
import rx.joins.Plan0;
import rx.joins.operators.OperatorJoinPatterns;

public class JoinObservable<T> {

    private final Observable<T> o;

    private JoinObservable(Observable<T> o) {
        this.o = o;
    }

    public static <T> JoinObservable<T> from(Observable<T> o) {
        return new JoinObservable<T>(o);
    }

    /**
     * Returns a Pattern that matches when both Observables emit an item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param right
     *            an Observable to match with the source Observable
     * @return a Pattern object that matches when both Observables emit an item
     * @throws NullPointerException
     *             if {@code right} is null
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: and()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229153.aspx">MSDN: Observable.And</a>
     */
    public final <T2> Pattern2<T, T2> and(Observable<T2> right) {
        return OperatorJoinPatterns.and(o, right);
    }

    /**
     * Joins together the results from several patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param plans
     *            a series of plans created by use of the {@link #then} Observer on patterns
     * @return an Observable that emits the results from matching several patterns
     * @throws NullPointerException
     *             if {@code plans} is null
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229558.aspx">MSDN: Observable.When</a>
     */
    public final static <R> JoinObservable<R> when(Iterable<? extends Plan0<R>> plans) {
        if (plans == null) {
            throw new NullPointerException("plans");
        }
        return from(Observable.create(OperatorJoinPatterns.when(plans)));
    }

    /**
     * Joins together the results from several patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param plans
     *            a series of plans created by use of the {@link #then} Observer on patterns
     * @return an Observable that emits the results from matching several patterns
     * @throws NullPointerException
     *             if {@code plans} is null
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    public final static <R> JoinObservable<R> when(Plan0<R>... plans) {
        return from(Observable.create(OperatorJoinPatterns.when(plans)));
    }

    /**
     * Joins the results from a pattern via its plan.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            the plan to join, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching a pattern
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1) {
        return from(Observable.create(OperatorJoinPatterns.when(p1)));
    }

    /**
     * Joins together the results from two patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching two patterns
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1, Plan0<R> p2) {
        return from(Observable.create(OperatorJoinPatterns.when(p1, p2)));
    }

    /**
     * Joins together the results from three patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching three patterns
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1, Plan0<R> p2, Plan0<R> p3) {
        return from(Observable.create(OperatorJoinPatterns.when(p1, p2, p3)));
    }

    /**
     * Joins together the results from four patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching four patterns
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1, Plan0<R> p2, Plan0<R> p3, Plan0<R> p4) {
        return from(Observable.create(OperatorJoinPatterns.when(p1, p2, p3, p4)));
    }

    /**
     * Joins together the results from five patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching five patterns
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1, Plan0<R> p2, Plan0<R> p3, Plan0<R> p4, Plan0<R> p5) {
        return from(Observable.create(OperatorJoinPatterns.when(p1, p2, p3, p4, p5)));
    }

    /**
     * Joins together the results from six patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p6
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching six patterns
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1, Plan0<R> p2, Plan0<R> p3, Plan0<R> p4, Plan0<R> p5, Plan0<R> p6) {
        return from(Observable.create(OperatorJoinPatterns.when(p1, p2, p3, p4, p5, p6)));
    }

    /**
     * Joins together the results from seven patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p6
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p7
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching seven patterns
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1, Plan0<R> p2, Plan0<R> p3, Plan0<R> p4, Plan0<R> p5, Plan0<R> p6, Plan0<R> p7) {
        return from(Observable.create(OperatorJoinPatterns.when(p1, p2, p3, p4, p5, p6, p7)));
    }

    /**
     * Joins together the results from eight patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p6
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p7
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p8
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching eight patterns
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1, Plan0<R> p2, Plan0<R> p3, Plan0<R> p4, Plan0<R> p5, Plan0<R> p6, Plan0<R> p7, Plan0<R> p8) {
        return from(Observable.create(OperatorJoinPatterns.when(p1, p2, p3, p4, p5, p6, p7, p8)));
    }

    /**
     * Joins together the results from nine patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p6
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p7
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p8
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p9
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching nine patterns
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public final static <R> JoinObservable<R> when(Plan0<R> p1, Plan0<R> p2, Plan0<R> p3, Plan0<R> p4, Plan0<R> p5, Plan0<R> p6, Plan0<R> p7, Plan0<R> p8, Plan0<R> p9) {
        return from(Observable.create(OperatorJoinPatterns.when(p1, p2, p3, p4, p5, p6, p7, p8, p9)));
    }

    /**
     * Matches when the Observable has an available item and projects the item by invoking the selector
     * function.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/and_then_when.png">
     * 
     * @param selector
     *            selector that will be invoked for items emitted by the source Observable
     * @return a {@link Plan0} that produces the projected results, to be fed (with other Plans) to the {@link #when} Observer
     * @throws NullPointerException
     *             if {@code selector} is null
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: then()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211662.aspx">MSDN: Observable.Then</a>
     */
    public final <R> Plan0<R> then(Func1<T, R> selector) {
        return OperatorJoinPatterns.then(o, selector);
    }
    
    public Observable<T> toObservable() {
        return o;
    }
}

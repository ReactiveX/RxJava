package rx.operators;

import rx.Observer;
import rx.util.functions.Func1;

public interface Operator<R, T> extends Func1<Observer<? super R>, Observer<? super T>> {

}

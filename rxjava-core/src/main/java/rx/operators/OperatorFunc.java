package rx.operators;

import rx.Operator;
import rx.util.functions.Func1;

public interface OperatorFunc<R, T> extends Func1<Operator<? super R>, Operator<? super T>> {

}

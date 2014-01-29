package rx.operators;

import rx.Subscriber;
import rx.util.functions.Func1;

public interface Operator<R, T> extends Func1<Subscriber<? super R>, Subscriber<? super T>> {

}

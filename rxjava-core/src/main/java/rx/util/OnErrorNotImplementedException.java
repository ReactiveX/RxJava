package rx.util;

import rx.Observer;

/**
 * Used for re-throwing {@link Observer#onError(Throwable)} when an implementation doesn't exist.
 * 
 * https://github.com/Netflix/RxJava/issues/198
 * 
 * Rx Design Guidelines 5.2
 * 
 * "when calling the Subscribe method that only has an onNext argument, the OnError behavior will be
 * to rethrow the exception on the thread that the message comes out from the observable sequence.
 * The OnCompleted behavior in this case is to do nothing."
 */
public class OnErrorNotImplementedException extends RuntimeException {
    private static final long serialVersionUID = -6298857009889503852L;

    public OnErrorNotImplementedException(String message, Throwable e) {
        super(message, e);
    }

    public OnErrorNotImplementedException(Throwable e) {
        super(e.getMessage(), e);
    }
}
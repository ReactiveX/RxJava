package rx.internal.operators;

import rx.Single;
import rx.SingleSubscriber;
import rx.exceptions.Exceptions;
import rx.functions.Func1;

public class SingleOperatorOnErrorResumeNextViaFunction<T> implements Single.OnSubscribe<T> {

    private final Single<? extends T> originalSingle;
    private final Func1<Throwable, ? extends Single<? extends T>> resumeFunction;

    public SingleOperatorOnErrorResumeNextViaFunction(Single<? extends T> originalSingle, Func1<Throwable, ? extends Single<? extends T>> resumeFunction) {
        if (originalSingle == null) {
            throw new NullPointerException("originalSingle must not be null");
        }

        if (resumeFunction == null) {
            throw new NullPointerException("resumeFunction must not be null");
        }

        this.originalSingle = originalSingle;
        this.resumeFunction = resumeFunction;
    }

    @Override
    public void call(final SingleSubscriber<? super T> child) {
        final SingleSubscriber<? super T> parent = new SingleSubscriber<T>() {
            @Override
            public void onSuccess(T value) {
                child.onSuccess(value);
            }

            @Override
            public void onError(Throwable error) {
                try {
                    unsubscribe();
                    resumeFunction.call(error).subscribe(child);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    child.onError(e);
                }
            }
        };

        child.add(parent);
        originalSingle.subscribe(parent);
    }
}

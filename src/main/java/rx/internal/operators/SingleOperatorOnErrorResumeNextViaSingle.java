package rx.internal.operators;

import rx.Single;
import rx.SingleSubscriber;
import rx.plugins.RxJavaPlugins;

public class SingleOperatorOnErrorResumeNextViaSingle<T> implements Single.OnSubscribe<T> {

    private final Single<? extends T> originalSingle;
    private final Single<? extends T> resumeSingleInCaseOfError;

    public SingleOperatorOnErrorResumeNextViaSingle(Single<? extends T> originalSingle, Single<? extends T> resumeSingleInCaseOfError) {
        if (originalSingle == null) {
            throw new NullPointerException("originalSingle must not be null");
        }

        if (resumeSingleInCaseOfError == null) {
            throw new NullPointerException("resumeSingleInCaseOfError must not be null");
        }

        this.originalSingle = originalSingle;
        this.resumeSingleInCaseOfError = resumeSingleInCaseOfError;
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
                RxJavaPlugins.getInstance().getErrorHandler().handleError(error);
                unsubscribe();

                resumeSingleInCaseOfError.subscribe(child);
            }
        };

        child.add(parent);
        originalSingle.subscribe(parent);
    }
}

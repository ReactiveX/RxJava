package rx.observers;

import rx.Observer;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action0;
import rx.functions.Action1;

public class Observers {

    private static final Observer<Object> EMPTY = new Observer<Object>() {

        @Override
        public final void onCompleted() {
            // do nothing
        }

        @Override
        public final void onError(Throwable e) {
            throw new OnErrorNotImplementedException(e);
        }

        @Override
        public final void onNext(Object args) {
            // do nothing
        }

    };

    @SuppressWarnings("unchecked")
    public static <T> Observer<T> empty() {
        return (Observer<T>) EMPTY;
    }

    /**
     * Create an Observer that receives `onNext` and ignores `onError` and `onCompleted`.
     */
    public static final <T> Observer<T> create(final Action1<? super T> onNext) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

    /**
     * Create an Observer that receives `onNext` and `onError` and ignores `onCompleted`.
     * 
     */
    public static final <T> Observer<T> create(final Action1<? super T> onNext, final Action1<Throwable> onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

    /**
     * Create an Observer that receives `onNext`, `onError` and `onCompleted`.
     * 
     */
    public static final <T> Observer<T> create(final Action1<? super T> onNext, final Action1<Throwable> onError, final Action0 onComplete) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                onComplete.call();
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

}

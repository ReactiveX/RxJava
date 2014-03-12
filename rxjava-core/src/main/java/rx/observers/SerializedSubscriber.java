package rx.observers;

import rx.Observer;
import rx.Subscriber;

public class SerializedSubscriber<T> extends Subscriber<T> {

    private final Observer<T> s;

    public SerializedSubscriber(Subscriber<? super T> s) {
        this.s = new SerializedObserverViaStateMachine<T>(s);
    }

    @Override
    public void onCompleted() {
        s.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        s.onError(e);
    }

    @Override
    public void onNext(T t) {
        s.onNext(t);
    }
}

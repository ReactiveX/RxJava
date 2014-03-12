package rx.observers;

import rx.Observer;

public class SerializedObserverViaQueueAndLockTest extends SerializedObserverTest {
    @Override
    protected Observer<String> serializedObserver(Observer<String> o) {
        return new SerializedObserverViaQueueAndLock<String>(o);
    }

}

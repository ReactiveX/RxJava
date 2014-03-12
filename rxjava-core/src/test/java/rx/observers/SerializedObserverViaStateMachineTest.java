package rx.observers;

import rx.Observer;

public class SerializedObserverViaStateMachineTest extends SerializedObserverTest {
    @Override
    protected Observer<String> serializedObserver(Observer<String> o) {
        return new SerializedObserverViaStateMachine<String>(o);
    }
}

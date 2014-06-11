package rx.usecases;

import java.util.Iterator;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;

public class PerfBaseline {

    @GenerateMicroBenchmark
    public void forLoopConsumption(UseCaseInput input) throws InterruptedException {
        for (int i = 0; i < input.size; i++) {
            input.observer.onNext(i);
        }
    }

    @GenerateMicroBenchmark
    public void observableConsumption(UseCaseInput input) throws InterruptedException {
        input.firehose.subscribe(input.observer);
        input.awaitCompletion();
    }
    
    @GenerateMicroBenchmark
    public void observableViaRange(UseCaseInput input) throws InterruptedException {
        input.observable.subscribe(input.observer);
        input.awaitCompletion();
    }

    @GenerateMicroBenchmark
    public void iterableViaForLoopConsumption(UseCaseInput input) throws InterruptedException {
        for (int i : input.iterable) {
            input.observer.onNext(i);
        }
    }

    @GenerateMicroBenchmark
    public void iterableViaHasNextConsumption(UseCaseInput input) throws InterruptedException {
        Iterator<Integer> iterator = input.iterable.iterator();
        while (iterator.hasNext()) {
            input.observer.onNext(iterator.next());
        }
    }
}

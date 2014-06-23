package rx;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import rx.jmh.InputWithIncrementingInteger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class PerfBaseline {

    @State(Scope.Thread)
    public static class Input extends InputWithIncrementingInteger {

        @Param({ "1", "1000", "1000000" })
        public int size;

        @Override
        public int getSize() {
            return size;
        }

    }

    @GenerateMicroBenchmark
    public void observableConsumption(Input input) throws InterruptedException {
        input.firehose.subscribe(input.observer);
    }

    @GenerateMicroBenchmark
    public void observableViaRange(Input input) throws InterruptedException {
        input.observable.subscribe(input.observer);
    }

    @GenerateMicroBenchmark
    public void iterableViaForLoopConsumption(Input input) throws InterruptedException {
        for (int i : input.iterable) {
            input.observer.onNext(i);
        }
    }

    @GenerateMicroBenchmark
    public void iterableViaHasNextConsumption(Input input) throws InterruptedException {
        Iterator<Integer> iterator = input.iterable.iterator();
        while (iterator.hasNext()) {
            input.observer.onNext(iterator.next());
        }
    }
}

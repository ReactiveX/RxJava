package rx.operators;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import rx.Observable;
import rx.Subscriber;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OperatorRangePerf {

    @Benchmark
    public void rangeWithBackpressureRequest(InputUsingRequest input) throws InterruptedException {
        input.observable.subscribe(input.newSubscriber());
    }

    @State(Scope.Thread)
    public static class InputUsingRequest {

        @Param({ "1", "1000", "1000000" })
        public int size;

        public Observable<Integer> observable;
        Blackhole bh;

        @Setup
        public void setup(final Blackhole bh) {
            observable = Observable.range(0, size);
            this.bh = bh;
        }

        public Subscriber<Integer> newSubscriber() {
            return new Subscriber<Integer>(size) {

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer t) {
                    bh.consume(t);
                }

            };
        }

    }

    @Benchmark
    public void rangeWithoutBackpressure(InputWithoutRequest input) throws InterruptedException {
        input.observable.subscribe(input.newSubscriber());
    }

    @State(Scope.Thread)
    public static class InputWithoutRequest {

        @Param({ "1", "1000", "1000000" })
        public int size;

        public Observable<Integer> observable;
        Blackhole bh;

        @Setup
        public void setup(final Blackhole bh) {
            observable = Observable.range(0, size);
            this.bh = bh;

        }

        public Subscriber<Integer> newSubscriber() {
            return new Subscriber<Integer>() {

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer t) {
                    bh.consume(t);
                }

            };
        }

    }

}

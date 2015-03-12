package rx.operators;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import rx.Observable;
import rx.functions.Func1;
import rx.jmh.InputWithIncrementingInteger;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OperatorMatchPerf {
    @State(Scope.Thread)
    public static class Input extends InputWithIncrementingInteger {

        @Param({ "1", "1000", "1000000" })
        public int size;

        @Override
        public int getSize() {
            return size;
        }

    }

    @Benchmark
    public void mapPassThruBaseline(Input input) {
        input.observable.map(BASELINE_FUNC);
    }

    @Benchmark
    public void flapMapPassThru(Input input) {
        input.observable.flatMap(new Func1<Integer, Observable<?>>() {
            @Override
            public Observable<?> call(Integer integer) {
                if (EVEN_PREDICATE.call(integer)) {
                    return Observable.just(TIMES_TWO.call(integer));
                } else if (DIVISIBLE_BY_THREE_PREDICATE.call(integer)) {
                    return Observable.just(TIMES_THREE.call(integer));
                } else {
                    return Observable.just(TIMES_FOUR.call(integer));
                }
            }
        });
    }

    @Benchmark
    public void multipleMatchAndDefaultPassThru(Input input) {
        input
            .observable
            .match(EVEN_PREDICATE, TIMES_TWO)
            .match(DIVISIBLE_BY_THREE_PREDICATE, TIMES_THREE)
            .matchDefault(TIMES_FOUR);
    }

    private static final Func1<Integer, Integer> BASELINE_FUNC = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer integer) {
            if (integer % 2 == 0) {
                return integer * 2;
            } else if (integer % 3 == 0) {
                return integer * 3;
            } else {
                return integer * 4;
            }
        }
    };

    private static final Func1<Integer, Boolean> EVEN_PREDICATE = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer i) {
            return i % 2 == 0;
        }
    };

    private static final Func1<Integer, Boolean> DIVISIBLE_BY_THREE_PREDICATE = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer i) {
            return i % 3 == 0;
        }
    };

    private static final Func1<Integer, Integer> TIMES_TWO = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer integer) {
            return integer * 2;
        }
    };


    private static final Func1<Integer, Integer> TIMES_THREE = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer integer) {
            return integer * 3;
        }
    };


    private static final Func1<Integer, Integer> TIMES_FOUR = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer integer) {
            return integer * 4;
        }
    };


}

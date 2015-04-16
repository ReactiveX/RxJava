package rx.operators;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import rx.internal.operators.OperatorTakeLast;
import rx.internal.operators.OperatorTakeLastOne;
import rx.jmh.InputWithIncrementingInteger;

public class OperatorTakeLastOnePerf {

    private static final OperatorTakeLast<Integer> TAKE_LAST = new OperatorTakeLast<Integer>(1);

    @State(Scope.Thread)
    public static class Input extends InputWithIncrementingInteger {

        @Param({ "5", "100", "1000000" })
        public int size;

        @Override
        public int getSize() {
            return size;
        }

    }
    
    @Benchmark
    public void takeLastOneUsingTakeLast(Input input) {
       input.observable.lift(TAKE_LAST).subscribe(input.observer);
    }
    
    @Benchmark
    public void takeLastOneUsingTakeLastOne(Input input) {
       input.observable.lift(OperatorTakeLastOne.<Integer>instance()).subscribe(input.observer);
    }
    
}

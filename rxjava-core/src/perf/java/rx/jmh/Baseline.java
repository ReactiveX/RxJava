package rx.jmh;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import rx.functions.Func1;

public class Baseline {

    @GenerateMicroBenchmark
    public void forLoopInvokingFunction(BlackHole bh, Input input) {
        for (int value = 0; value < input.size; value++) {
            bh.consume(IDENTITY_FUNCTION.call(value));
        }
    }

    private static final Func1<Integer, Integer> IDENTITY_FUNCTION = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer value) {
            return value;
        }
    };

    @State(Scope.Thread)
    public static class Input {

        @Param({ "1024", "1048576" })
        public int size;

    }

}

package rx.exceptions;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ExceptionsThrowIfFatalPerf {

    @State(Scope.Thread)
    public static class StateWithIOException {
        public Throwable throwable = new IOException();
    }

    @State(Scope.Thread)
    public static class StateWithIllegalArgumentException {
        public Throwable throwable = new IllegalArgumentException();
    }

    @State(Scope.Thread)
    public static class StateWithOutOfMemoryError {
        public Throwable throwable = new OutOfMemoryError();
    }

    @Benchmark
    public Object throwIfFatalNonFatalCheckedException(StateWithIOException state) {
        try {
            Exceptions.throwIfFatal(state.throwable);
            return new Object();
        } catch (Throwable expected) {
            // We should return the result to exclude dead code elimination problem
            return expected;
        }
    }

    @Benchmark
    public Object throwIfFatalNonFatalRuntimeException(StateWithIllegalArgumentException state) {
        try {
            Exceptions.throwIfFatal(state.throwable);
            return new Object();
        } catch (Throwable expected) {
            // We should return the result to exclude dead code elimination problem
            return expected;
        }
    }

    @Benchmark
    public Object throwIfFatalNonFatalError(StateWithOutOfMemoryError state) {
        try {
            Exceptions.throwIfFatal(state.throwable);
            return new Object();
        } catch (Throwable expected) {
            // We should return the result to exclude dead code elimination problem
            return expected;
        }
    }
}

package rx.internal.util;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import rx.exceptions.MissingBackpressureException;

public class PerfRingBuffer {

    @GenerateMicroBenchmark
    public void onNextConsume(OnNextConsumeInput input) throws InterruptedException, MissingBackpressureException {
        RxSpscRingBuffer queue = new RxSpscRingBuffer();
        for (int i = 0; i < input.size; i++) {
            queue.onNext(i);
        }
        for (int i = 0; i < input.size; i++) {
            input.bh.consume(queue.poll());
        }
    }

    @State(Scope.Thread)
    public static class OnNextConsumeInput {
        @Param({ "100", "1023" })
        public int size;
        public BlackHole bh;

        @Setup
        public void setup(final BlackHole bh) {
            this.bh = bh;
        }
    }

    @GenerateMicroBenchmark
    public void onNextPollLoop(OnNextPollLoopInput input) throws InterruptedException, MissingBackpressureException {
        RxSpscRingBuffer queue = new RxSpscRingBuffer();
        for (int i = 0; i < input.size; i++) {
            queue.onNext(i);
            input.bh.consume(queue.poll());
        }
    }

    @State(Scope.Thread)
    public static class OnNextPollLoopInput {
        @Param({ "100", "1000000" })
        public int size;
        public BlackHole bh;

        @Setup
        public void setup(final BlackHole bh) {
            this.bh = bh;
        }
    }

}

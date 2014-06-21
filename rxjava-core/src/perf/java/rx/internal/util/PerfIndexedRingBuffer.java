package rx.internal.util;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import rx.exceptions.MissingBackpressureException;

public class PerfIndexedRingBuffer {

    @GenerateMicroBenchmark
    public void indexedRingBufferAdd(IndexedRingBufferInput input) throws InterruptedException, MissingBackpressureException {
        IndexedRingBuffer<Integer> list = IndexedRingBuffer.getInstance();
        for (int i = 0; i < input.size; i++) {
            list.add(i);
        }

        list.unsubscribe();
    }

    @GenerateMicroBenchmark
    public void indexedRingBufferAddRemove(IndexedRingBufferInput input) throws InterruptedException, MissingBackpressureException {
        IndexedRingBuffer<Integer> list = IndexedRingBuffer.getInstance();
        for (int i = 0; i < input.size; i++) {
            list.add(i);
        }

        for (int i = 0; i < input.size; i++) {
            list.remove(i);
        }

        list.unsubscribe();
    }

    @State(Scope.Thread)
    public static class IndexedRingBufferInput {
        @Param({ "100", "10000" })
        public int size;
        public BlackHole bh;

        @Setup
        public void setup(final BlackHole bh) {
            this.bh = bh;
        }
    }

}

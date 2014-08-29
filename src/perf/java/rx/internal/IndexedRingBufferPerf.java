package rx.internal;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import rx.exceptions.MissingBackpressureException;
import rx.internal.util.IndexedRingBuffer;

public class IndexedRingBufferPerf {

    @Benchmark
    public void indexedRingBufferAdd(IndexedRingBufferInput input) throws InterruptedException, MissingBackpressureException {
        IndexedRingBuffer<Integer> list = IndexedRingBuffer.getInstance();
        for (int i = 0; i < input.size; i++) {
            list.add(i);
        }

        list.unsubscribe();
    }

    @Benchmark
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
        public Blackhole bh;

        @Setup
        public void setup(final Blackhole bh) {
            this.bh = bh;
        }
    }

}

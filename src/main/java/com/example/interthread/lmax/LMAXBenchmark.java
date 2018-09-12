package com.example.interthread.lmax;

import com.example.interthread.LongEvent;
import com.example.interthread.NamedThreadFactory;
import com.example.interthread.lmax.LongEventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class LMAXBenchmark {

    private static final int BUFFER_SIZE = 1024 * 1024;

    @State(Scope.Benchmark)
    public static class LMAXBenchmarkState {

        @Param(value = {"1000", "10000", "100000", "10000000", "50000000", "500000000"})
        long nrEvents;

        RingBuffer<LongEvent> ringBuffer;
        private Disruptor<LongEvent> disruptor;
        private ByteBuffer byteBuffer;

        @Setup(Level.Trial)
        public void setUp() {
            LongEventFactory eventFactory = new LongEventFactory();
            disruptor = new Disruptor<>(eventFactory, BUFFER_SIZE, new NamedThreadFactory(), ProducerType.SINGLE, new /*BusySpinWaitStrategy*/ YieldingWaitStrategy());
            disruptor.handleEventsWith( (event, sequence, endOfBatch) -> {} );

            disruptor.start();

            ringBuffer = disruptor.getRingBuffer();
            byteBuffer = ByteBuffer.allocate(8);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            disruptor.shutdown();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 1)
    @Measurement(iterations = 5)
    public void lmaxAverageTime(LMAXBenchmarkState state) {
        for (long num=0; num<state.nrEvents; num++) {
            state.byteBuffer.putLong(0, num);
            state.ringBuffer.publishEvent((event, sequence, bb) -> event.setValue(state.byteBuffer.getLong(0)), state.byteBuffer);
        }
    }
}

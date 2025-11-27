package org.fressian.benchmark;

import org.fressian.impl.ChunkedList;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark focused on measuring memory allocation.
 * 
 * Run with: java -jar target/benchmarks.jar AllocationBenchmark -prof gc
 * 
 * The -prof gc profiler will show:
 * - gc.alloc.rate: allocation rate in MB/sec
 * - gc.alloc.rate.norm: bytes allocated per operation (MOST IMPORTANT)
 * - gc.count: number of GC events
 * - gc.time: time spent in GC
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms4G", "-Xmx4G"})
public class AllocationBenchmark {

    @Param({"10000", "50000", "100000", "200000"})
    private int listSize;
    
    // Pre-created elements (Long objects) to isolate list allocation from element allocation
    private Object[] elementsToAdd;
    
    @Setup(Level.Trial)
    public void setup() {
        elementsToAdd = new Object[listSize];
        for (int i = 0; i < listSize; i++) {
            elementsToAdd[i] = Long.valueOf(i);
        }
    }
    
    /**
     * ArrayList with default capacity - the current FressianReader behavior.
     * Expected: ~3.2x listSize bytes allocated for list structure
     */
    @Benchmark
    public Object[] arrayListDefault() {
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            list.add(elementsToAdd[i]);
        }
        return list.toArray();
    }
    
    /**
     * ChunkedList - proposed optimization.
     * Expected: ~2x listSize bytes allocated for list structure
     */
    @Benchmark
    public Object[] chunkedList() {
        ChunkedList list = new ChunkedList();
        for (int i = 0; i < listSize; i++) {
            list.add(elementsToAdd[i]);
        }
        return list.toArray();
    }
    
    /**
     * Direct array allocation (theoretical minimum).
     * This is what we'd get if we knew the size upfront.
     */
    @Benchmark
    public Object[] directArray() {
        Object[] result = new Object[listSize];
        for (int i = 0; i < listSize; i++) {
            result[i] = elementsToAdd[i];
        }
        return result;
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(AllocationBenchmark.class.getSimpleName())
                .addProfiler("gc")  // Enable GC profiler to measure allocations
                .build();
        new Runner(opt).run();
    }
}


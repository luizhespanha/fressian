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
 * Benchmark comparing ArrayList vs ChunkedList allocation overhead.
 * 
 * Tests a range of list sizes from tiny (10 elements) to large (250K elements)
 * to verify behavior across all use cases.
 * 
 * Metrics reported by JMH gc profiler:
 * - gc.alloc.rate.norm: bytes allocated per operation (normalized)
 * 
 * Run with: java -jar target/benchmarks.jar RealisticAllocationBenchmark -prof gc
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms4G", "-Xmx4G"})
public class RealisticAllocationBenchmark {

    // Range of sizes: small lists to large lists from production JFR data
    @Param({
        "10",      // Tiny lists
        "100",     // Small lists
        "1000",    // Medium lists
        "10000",   // Large lists
        "30000",   // P10 from JFR
        "100000",  // ~P20 from JFR
        "187500",  // P50 from JFR - MEDIAN
        "250000"   // P75 from JFR
    })
    private int listSize;
    
    // Pre-created elements (Long objects) - simulates Long boxing in Fressian
    private Object[] elementsToAdd;
    
    @Setup(Level.Trial)
    public void setup() {
        elementsToAdd = new Object[listSize];
        for (int i = 0; i < listSize; i++) {
            // Simulate Long boxing like in Fressian readClosedList
            elementsToAdd[i] = Long.valueOf(i);
        }
    }
    
    /**
     * ArrayList with default capacity - CURRENT FressianReader behavior.
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
     * ChunkedList - PROPOSED optimization.
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
     * Direct array - theoretical minimum (if we knew size upfront).
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
                .include(RealisticAllocationBenchmark.class.getSimpleName())
                .addProfiler("gc")
                .build();
        new Runner(opt).run();
    }
}


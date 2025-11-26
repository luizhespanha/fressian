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
 * Benchmark with REALISTIC list sizes based on production JFR data.
 * 
 * JFR Analysis showed:
 * - P10:  ~30K elements  (arrays of ~250 kB)
 * - P25:  ~137K elements (arrays of ~1.1 MB)
 * - P50:  ~187K elements (arrays of ~1.5 MB) - MEDIAN
 * - P75:  ~250K elements (arrays of ~2.0 MB)
 * - P90:  ~312K elements (arrays of ~2.5 MB)
 * - P99:  ~487K elements (arrays of ~3.9 MB)
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

    // Realistic sizes from production JFR data
    @Param({
        "30000",   // P10 - small lists
        "137500",  // P25 
        "187500",  // P50 - MEDIAN (most common)
        "250000",  // P75
        "312500",  // P90
        "487500"   // P99 - large lists
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


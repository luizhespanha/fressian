package org.fressian.benchmark;

import org.fressian.impl.ChunkedList;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparing ArrayList vs ChunkedList for the readClosedList pattern:
 * - Add N elements one by one (unknown size upfront)
 * - Convert to Object[] at the end
 * 
 * This simulates exactly what FressianReader.readClosedList() does.
 * 
 * Run with: mvn clean install -DskipTests && java -jar target/benchmarks.jar
 * Or: mvn exec:java -Dexec.mainClass="org.fressian.benchmark.ListGrowthBenchmark"
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ListGrowthBenchmark {

    @Param({"100", "1000", "10000", "50000", "100000"})
    private int listSize;
    
    // Pre-create objects to add (so we measure list growth, not object creation)
    private Object[] elementsToAdd;
    
    @Setup(Level.Trial)
    public void setup() {
        elementsToAdd = new Object[listSize];
        for (int i = 0; i < listSize; i++) {
            // Simulate Long boxing like in Fressian
            elementsToAdd[i] = Long.valueOf(i);
        }
    }
    
    /**
     * Baseline: ArrayList with default initial capacity (10)
     * This is what FressianReader currently does.
     */
    @Benchmark
    public Object[] arrayListDefault(Blackhole bh) {
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            list.add(elementsToAdd[i]);
        }
        return list.toArray();
    }
    
    /**
     * ArrayList with larger initial capacity (64)
     * A simple optimization that doesn't help much for large lists.
     */
    @Benchmark
    public Object[] arrayList64(Blackhole bh) {
        ArrayList<Object> list = new ArrayList<>(64);
        for (int i = 0; i < listSize; i++) {
            list.add(elementsToAdd[i]);
        }
        return list.toArray();
    }
    
    /**
     * ArrayList with perfect initial capacity (cheating - knows size upfront)
     * This shows the theoretical minimum for ArrayList.
     */
    @Benchmark
    public Object[] arrayListPerfect(Blackhole bh) {
        ArrayList<Object> list = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            list.add(elementsToAdd[i]);
        }
        return list.toArray();
    }
    
    /**
     * ChunkedList: grows in fixed chunks, no copying during growth.
     * This is our proposed optimization.
     */
    @Benchmark
    public Object[] chunkedList(Blackhole bh) {
        ChunkedList list = new ChunkedList();
        for (int i = 0; i < listSize; i++) {
            list.add(elementsToAdd[i]);
        }
        return list.toArray();
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ListGrowthBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}


package org.fressian.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.fressian.impl.ChunkedList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing allocation strategies for list building during deserialization:
 * 
 * 1. ArrayList (baseline) - standard ArrayList with default capacity
 * 2. ChunkedList - custom chunked implementation (current PR)
 * 3. PooledArrayList - ThreadLocal pool of ArrayLists (Netty-style)
 * 
 * We measure:
 * - gc.alloc.rate.norm: bytes allocated per operation
 * - Throughput: operations per second
 * 
 * Run with:
 *   mvn clean package -DskipTests
 *   java -jar target/benchmarks.jar PoolVsChunkedBenchmark -prof gc
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class PoolVsChunkedBenchmark {
    
    @Param({
        "10",       // tiny list
        "100",      // small list
        "1000",     // medium list
        "10000",    // large list (P10 from production)
        "100000"    // very large list (P50 from production)
    })
    private int listSize;
    
    // Pool for PooledArrayList approach
    private ArrayDeque<ArrayList<Object>> pool;
    
    // Dummy object to add to lists
    private static final Object DUMMY = new Object();
    
    @Setup(Level.Iteration)
    public void setup() {
        // Reset pool for each iteration to ensure fair comparison
        pool = new ArrayDeque<>();
    }
    
    // ========== BASELINE: ArrayList ==========
    
    @Benchmark
    public Object[] arrayList(Blackhole bh) {
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            list.add(DUMMY);
        }
        return list.toArray();
    }
    
    @Benchmark
    public Object[] arrayListPresized(Blackhole bh) {
        ArrayList<Object> list = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            list.add(DUMMY);
        }
        return list.toArray();
    }
    
    // ========== CURRENT: ChunkedList ==========
    
    @Benchmark
    public Object[] chunkedList(Blackhole bh) {
        ChunkedList list = new ChunkedList();
        for (int i = 0; i < listSize; i++) {
            list.add(DUMMY);
        }
        return list.toArray();
    }
    
    // ========== NEW: Pooled ArrayList ==========
    
    @Benchmark
    public Object[] pooledArrayList(Blackhole bh) {
        ArrayList<Object> list = acquireFromPool();
        try {
            for (int i = 0; i < listSize; i++) {
                list.add(DUMMY);
            }
            return list.toArray();
        } finally {
            releaseToPool(list);
        }
    }
    
    private ArrayList<Object> acquireFromPool() {
        ArrayList<Object> list = pool.pollFirst();
        if (list == null) {
            list = new ArrayList<>(64);
        }
        return list;
    }
    
    private void releaseToPool(ArrayList<Object> list) {
        list.clear();
        if (pool.size() < 8) {
            pool.addFirst(list);
        }
    }
    
    // ========== THEORETICAL MINIMUM: Direct Array ==========
    
    @Benchmark
    public Object[] directArray(Blackhole bh) {
        // This is the theoretical minimum - just allocate the final array
        Object[] result = new Object[listSize];
        for (int i = 0; i < listSize; i++) {
            result[i] = DUMMY;
        }
        return result;
    }
    
    // ========== RECURSION TEST ==========
    // Simulates nested list reading: [[1,2,3], [4,5,6], [7,8,9]]
    
    @Benchmark
    public Object[] pooledRecursive(Blackhole bh) {
        return readNestedList(3, 100);  // 3 levels deep, 100 elements each
    }
    
    @Benchmark
    public Object[] chunkedRecursive(Blackhole bh) {
        return readNestedListChunked(3, 100);
    }
    
    private Object[] readNestedList(int depth, int elementsPerLevel) {
        ArrayList<Object> list = acquireFromPool();
        try {
            for (int i = 0; i < elementsPerLevel; i++) {
                if (depth > 1) {
                    list.add(readNestedList(depth - 1, elementsPerLevel));
                } else {
                    list.add(DUMMY);
                }
            }
            return list.toArray();
        } finally {
            releaseToPool(list);
        }
    }
    
    private Object[] readNestedListChunked(int depth, int elementsPerLevel) {
        ChunkedList list = new ChunkedList();
        for (int i = 0; i < elementsPerLevel; i++) {
            if (depth > 1) {
                list.add(readNestedListChunked(depth - 1, elementsPerLevel));
            } else {
                list.add(DUMMY);
            }
        }
        return list.toArray();
    }
}


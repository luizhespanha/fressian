#!/bin/bash

# Script to run allocation benchmarks comparing ArrayList vs ChunkedList
# 
# Usage:
#   ./run-benchmarks.sh           # Run all benchmarks
#   ./run-benchmarks.sh quick     # Quick run (fewer iterations)
#   ./run-benchmarks.sh alloc     # Run only allocation benchmark with GC profiler

set -e

echo "========================================"
echo "Building project..."
echo "========================================"
mvn clean compile -DskipTests -q

echo ""
echo "========================================"
echo "Running benchmarks..."
echo "========================================"

CLASSPATH=$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q):target/classes

case "${1:-full}" in
    quick)
        echo "Running QUICK benchmarks (fewer iterations)..."
        java -cp "$CLASSPATH" org.openjdk.jmh.Main \
            -wi 1 -i 3 -f 1 \
            -p listSize=10000,100000 \
            ".*Benchmark.*"
        ;;
    alloc)
        echo "Running ALLOCATION benchmarks with GC profiler..."
        java -cp "$CLASSPATH" -Xms4G -Xmx4G org.openjdk.jmh.Main \
            -wi 2 -i 5 -f 2 \
            -prof gc \
            "AllocationBenchmark"
        ;;
    full)
        echo "Running FULL benchmarks..."
        java -cp "$CLASSPATH" -Xms4G -Xmx4G org.openjdk.jmh.Main \
            -prof gc \
            ".*Benchmark.*"
        ;;
    *)
        echo "Unknown option: $1"
        echo "Usage: $0 [quick|alloc|full]"
        exit 1
        ;;
esac

echo ""
echo "========================================"
echo "Benchmark complete!"
echo "========================================"


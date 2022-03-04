package dev.mccue.example;

import org.openjdk.jmh.annotations.Benchmark;

public class ExampleBenchmark {

    @Benchmark
    public void testMethod() {
        int a = 1;
        int b = 2;
        int sum = a + b;
    }
}

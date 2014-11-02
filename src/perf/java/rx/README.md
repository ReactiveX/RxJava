# JMH Benchmarks

### Run All

```
./gradlew benchmarks
```

### Run Specific Class

```
./gradlew benchmarks '-Pjmh=.*OperatorMapPerf.*'
```

### Arguments

Optionally pass arguments for custom execution. Example:

```
./gradlew benchmarks '-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*OperatorMapPerf.*'
```

gives output like this:

```
# Warmup Iteration   1: 17541564.529 ops/s
# Warmup Iteration   2: 20923290.222 ops/s
# Warmup Iteration   3: 20743426.176 ops/s
# Warmup Iteration   4: 23462116.103 ops/s
# Warmup Iteration   5: 24283139.706 ops/s
Iteration   1: 23317338.378 ops/s
Iteration   2: 23478480.189 ops/s
Iteration   3: 23186077.043 ops/s
Iteration   4: 22120569.854 ops/s
Iteration   5: 21581828.652 ops/s

Result: 22736858.823 ±(99.9%) 3223211.259 ops/s [Average]
  Statistics: (min, avg, max) = (21581828.652, 22736858.823, 23478480.189), stdev = 837057.728
  Confidence interval (99.9%): [19513647.564, 25960070.082]
```

To see all options:

```
./gradlew benchmarks '-Pjmh=-h'
```

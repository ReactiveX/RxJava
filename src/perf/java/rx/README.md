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
./gradlew benchmarks '-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*OperatorMapPerf.*'
```

To see all options:

```
./gradlew benchmarks '-Pjmh=-h'
```

package io.reactivex.rxjava4.functions;

import io.reactivex.rxjava4.annotations.NonNull;

public record Args3<T1, T2, T3>(
        @NonNull T1 t1,
        @NonNull T2 t2,
        @NonNull T3 t3
) {}
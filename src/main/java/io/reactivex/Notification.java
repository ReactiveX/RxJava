/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;

import java.util.*;

/**
 * Utility class to help construct notification objects.
 */
public final class Notification {
    private Notification() {
        throw new IllegalStateException();
    }
    
    static final Try<Optional<?>> COMPLETE = Try.ofValue(Optional.empty());
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Try<Optional<T>> complete() {
        return (Try)COMPLETE;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Try<Optional<T>> error(Throwable e) {
        return (Try)Try.ofError(e);
    }
    
    public static <T> Try<Optional<T>> next(T value) {
        Objects.requireNonNull(value); // TODO this coud instead return an error of NPE
        return Try.ofValue(Optional.of(value));
    }
    
    public static <T> boolean isNext(Try<Optional<T>> notification) {
        if (notification.hasValue()) {
            return notification.value().isPresent();
        }
        return false;
    }
    
    public static <T> boolean isComplete(Try<Optional<T>> notification) {
        if (notification.hasValue()) {
            return !notification.value().isPresent();
        }
        return false;
    }
    
    public static <T> boolean isError(Try<Optional<T>> notification) {
        return notification.hasError();
    }
    
    public static <T> T getValue(Try<Optional<T>> notification) {
        if (notification.hasValue()) {
            return notification.value.get();
        }
        return null;
    }
}

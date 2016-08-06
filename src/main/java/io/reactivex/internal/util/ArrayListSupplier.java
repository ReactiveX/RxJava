package io.reactivex.internal.util;

import java.util.*;
import java.util.concurrent.Callable;

public enum ArrayListSupplier implements Callable<List<Object>> {
    INSTANCE;
  
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Callable<List<T>> instance() {
        return (Callable)INSTANCE;
    }
    
    @Override
    public List<Object> call() throws Exception {
        return new ArrayList<Object>();
    }
}

package io.reactivex.internal.operators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.reactivex.internal.util.BackpressureHelper;

public class BackpressureHelperTest {
    @Test
    public void testAddCap() {
        assertEquals(2L, BackpressureHelper.addCap(1, 1));
        assertEquals(Long.MAX_VALUE, BackpressureHelper.addCap(1, Long.MAX_VALUE - 1));
        assertEquals(Long.MAX_VALUE, BackpressureHelper.addCap(1, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, BackpressureHelper.addCap(Long.MAX_VALUE - 1, Long.MAX_VALUE - 1));
        assertEquals(Long.MAX_VALUE, BackpressureHelper.addCap(Long.MAX_VALUE, Long.MAX_VALUE));
    }
    
    @Test
    public void testMultiplyCap() {
        assertEquals(6, BackpressureHelper.multiplyCap(2, 3));
        assertEquals(Long.MAX_VALUE, BackpressureHelper.multiplyCap(2, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, BackpressureHelper.multiplyCap(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, BackpressureHelper.multiplyCap(1L << 32, 1L << 32));

    }
}
package rx.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

public class GenericScheduledExecutorServiceTest {
    @Test
    public void verifyInstanceIsSingleThreaded() throws Exception {
        ScheduledExecutorService exec = GenericScheduledExecutorService.getInstance();
        
        final AtomicInteger state = new AtomicInteger();

        final AtomicInteger found1 = new AtomicInteger();
        final AtomicInteger found2 = new AtomicInteger();
        
        Future<?> f1 = exec.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                found1.set(state.getAndSet(1));
            }
        }, 250, TimeUnit.MILLISECONDS);
        Future<?> f2 = exec.schedule(new Runnable() {
            @Override
            public void run() {
                found2.set(state.getAndSet(2));
            }
        }, 250, TimeUnit.MILLISECONDS);
        
        f1.get();
        f2.get();
        
        Assert.assertEquals(2, state.get());
        Assert.assertEquals(0, found1.get());
        Assert.assertEquals(1, found2.get());
    }
}

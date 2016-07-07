package rx.schedulers;

import java.util.concurrent.*;

import org.junit.*;

import rx.functions.Func0;
import rx.internal.schedulers.GenericScheduledExecutorService;
import rx.plugins.RxJavaHooks;

public class GenericScheduledExecutorServiceTest {

    @Test
    public void genericScheduledExecutorServiceHook() {
        // make sure the class is initialized
        Assert.assertNotNull(GenericScheduledExecutorService.class);
        
        final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            
            RxJavaHooks.setOnGenericScheduledExecutorService(new Func0<ScheduledExecutorService>() {
                @Override
                public ScheduledExecutorService call() {
                    return exec;
                }
            });
            
            Schedulers.shutdown();
            // start() is package private so had to move this test here
            Schedulers.start();
            
            Assert.assertSame(exec, GenericScheduledExecutorService.getInstance());
            
            RxJavaHooks.setOnGenericScheduledExecutorService(null);
            
            Schedulers.shutdown();
            // start() is package private so had to move this test here
            Schedulers.start();

            Assert.assertNotSame(exec, GenericScheduledExecutorService.getInstance());

        } finally {
            RxJavaHooks.reset();
            exec.shutdownNow();
        }
        
    }
}

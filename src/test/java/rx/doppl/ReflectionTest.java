package rx.doppl;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by kgalligan on 11/3/16.
 */

public class ReflectionTest
{
    @Test
    public void testGetMethods()
    {
//        Method[] methods = ScheduledThreadPoolExecutor.class.getMethods();
        Method[] methods = ExecutorService.class.getMethods();
        System.out.println("asdf");
    }
}

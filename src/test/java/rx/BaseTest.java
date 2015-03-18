package rx;

import java.lang.management.*;

import org.junit.*;

import rx.schedulers.Schedulers;
import rx.test.TestObstructionDetection;

/**
 * Extend this base class to detect test leaks.
 */
public abstract class BaseTest {
    long ioSchedulerCount;
    long beforeMemory;
    @Before
    public void baseTestBefore() {
        ioSchedulerCount = Schedulers.activeIOWorkers();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memHeap = memoryMXBean.getHeapMemoryUsage();
        beforeMemory = memHeap.getUsed();
    }
    @After
    public void baseTestAfter() {
        System.gc();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memHeap = memoryMXBean.getHeapMemoryUsage();
        long afterMemory = memHeap.getUsed();
        
        if (afterMemory > 6 * beforeMemory / 2) {
            try {
                Thread.sleep(500);
                System.gc();
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                // ignored
            }
            memHeap = memoryMXBean.getHeapMemoryUsage();
            afterMemory = memHeap.getUsed();
            
            if (afterMemory > 6 * beforeMemory / 2) {
                throw new AssertionError(String.format("Memory leak: %.3f -> %.3f", beforeMemory / 1024.0 / 1024.0, afterMemory / 1024.0 / 1024.0));
            }
        }
        
        long c = Schedulers.activeIOWorkers();
        if (c != ioSchedulerCount) {
            // give the test some time
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                // ignored
            }
            c = Schedulers.activeIOWorkers();
            if (c != ioSchedulerCount) {
                throw new AssertionError("IO scheduler leak! Before: " + ioSchedulerCount + ", After: " + c);
            }
        }
        TestObstructionDetection.checkObstruction();
    }
}

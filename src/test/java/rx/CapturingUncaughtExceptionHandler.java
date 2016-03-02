package rx;

import java.util.concurrent.CountDownLatch;

public final class CapturingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    public int count = 0;
    public Throwable caught;
    public CountDownLatch completed = new CountDownLatch(1);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        count++;
        caught = e;
        completed.countDown();
    }
}

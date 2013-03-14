package rx.concurrency;

import org.junit.Test;
import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class CurrentThreadScheduler extends AbstractScheduler {
    private static final CurrentThreadScheduler INSTANCE = new CurrentThreadScheduler();
    public static CurrentThreadScheduler getInstance() {
        return INSTANCE;
    }

    private static final ThreadLocal<Queue<DiscardableAction>> QUEUE = new ThreadLocal<Queue<DiscardableAction>>();

    private CurrentThreadScheduler() {
    }

    @Override
    public Subscription schedule(Func0<Subscription> action) {
        DiscardableAction discardableAction = new DiscardableAction(action);
        enqueue(discardableAction);
        return discardableAction;
    }

    private void enqueue(DiscardableAction action) {
        Queue<DiscardableAction> queue = QUEUE.get();
        boolean exec = false;

        if (queue == null) {
            queue = new LinkedList<DiscardableAction>();
            QUEUE.set(queue);
            exec = true;
        }

        queue.add(action);

        while (exec && !queue.isEmpty()) {
            queue.poll().call();
        }
    }

    public static class UnitTest {

        @Test
        public void testScheduler() {
            final CurrentThreadScheduler scheduler = new CurrentThreadScheduler();

            final Action0 firstAction = new Action0() {
                @Override
                public void call() {
                    System.out.println("First action start");
                    System.out.println("First action end");
                }
            };
            final Action0 secondAction = new Action0() {
                @Override
                public void call() {
                    System.out.println("Second action start");
                    scheduler.schedule(firstAction);
                    System.out.println("Second action end");

                }
            };
            final Action0 thirdAction = new Action0() {
                @Override
                public void call() {
                    System.out.println("Third action start");
                    scheduler.schedule(secondAction);
                    System.out.println("Third action end");
                }
            };

            scheduler.schedule(thirdAction);
        }

    }
}

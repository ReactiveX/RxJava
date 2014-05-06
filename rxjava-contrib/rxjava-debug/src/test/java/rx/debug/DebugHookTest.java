package rx.debug;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.functions.Func1;
import rx.observers.Subscribers;
import rx.plugins.DebugHook;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotification.Kind;
import rx.plugins.DebugNotificationListener;
import rx.plugins.PlugReset;
import rx.plugins.RxJavaPlugins;

public class DebugHookTest {
    @Before
    @After
    public void reset() {
        PlugReset.reset();
    }

    private static class TestDebugNotificationListener extends DebugNotificationListener<Object> {
        ConcurrentHashMap<Thread, AtomicInteger> allThreadDepths = new ConcurrentHashMap<Thread, AtomicInteger>(1);
        ThreadLocal<AtomicInteger> currentThreadDepth = new ThreadLocal<AtomicInteger>() {
            protected AtomicInteger initialValue() {
                AtomicInteger depth = new AtomicInteger();
                allThreadDepths.put(Thread.currentThread(), depth);
                return depth;
            };
        };

        @Override
        public <T> T onNext(DebugNotification<T> n) {
            if (n == null)
                return null; // because we are verifying on a spied object.
            System.err.println("next: " + n.getValue());
            return super.onNext(n);
        }

        @Override
        public <T> Object start(DebugNotification<T> n) {
            if (n == null)
                return null; // because we are verifying on a spied object.
            currentThreadDepth.get().incrementAndGet();
            Object context = new Object();
            System.err.println("start: " + Integer.toHexString(context.hashCode()) + " " + n);
            return context;
        }

        @Override
        public void complete(Object context) {
            if (context == null)
                return; // because we are verifying on a spied object.
            currentThreadDepth.get().decrementAndGet();
            System.err.println("complete: " + Integer.toHexString(context.hashCode()));
        }

        @Override
        public void error(Object context, Throwable e) {
            if (context == null)
                return; // because we are verifying on a spied object.
            currentThreadDepth.get().decrementAndGet();
            System.err.println("error: " + Integer.toHexString(context.hashCode()));
        }

        public void assertValidState() {
            for (Entry<Thread, AtomicInteger> threadDepth : allThreadDepths.entrySet()) {
                assertEquals(0, threadDepth.getValue().get());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Ignore
    @Test
    public void testSimple() {
        TestDebugNotificationListener listener = new TestDebugNotificationListener();
        listener = spy(listener);
        @SuppressWarnings("rawtypes")
        final DebugHook hook = new DebugHook(listener);
        RxJavaPlugins.getInstance().registerObservableExecutionHook(hook);

        Observable.from(1).subscribe(Subscribers.empty());

        final InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).start(subscribe());
        inOrder.verify(listener).onNext(onNext(1));
        inOrder.verify(listener).start(onNext(1));
        inOrder.verify(listener).complete(any());
        inOrder.verify(listener).start(onCompleted());
        inOrder.verify(listener, times(2)).complete(any());
        inOrder.verifyNoMoreInteractions();

        listener.assertValidState();
    }

    @SuppressWarnings("unchecked")
    @Ignore
    @Test
    public void testOneOp() {
        TestDebugNotificationListener listener = new TestDebugNotificationListener();
        listener = spy(listener);

        // create and register the hooks.
        final DebugHook<Object> hook = new DebugHook<Object>(listener);
        RxJavaPlugins.getInstance().registerObservableExecutionHook(hook);

        // do the operation
        Observable
                .from(Arrays.asList(1, 3))
                .flatMap(new Func1<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(Integer it) {
                        return Observable.from(Arrays.asList(it * 10, (it + 1) * 10));
                    }
                })
                .take(3)
                .subscribe(Subscribers.<Integer> empty());

        InOrder calls = inOrder(listener);

        calls.verify(listener).start(subscribe());
        calls.verify(listener).start(onNext(1)); // from to map
        calls.verify(listener).start(onNext(Observable.class)); // map to merge
        calls.verify(listener).start(subscribe()); // merge inner
        calls.verify(listener).start(onNext(10)); // from to merge inner
        calls.verify(listener).start(onNext(10)); // merge inner to take
        calls.verify(listener).start(onNext(10)); // take to empty subscriber
        calls.verify(listener, times(3)).complete(any());
        calls.verify(listener).start(onNext(20)); // next from to merge inner
        calls.verify(listener).start(onNext(20)); // merge inner to take
        calls.verify(listener).start(onNext(20)); // take to output
        calls.verify(listener, times(3)).complete(any());
        calls.verify(listener).start(onCompleted()); // sub from completes
        // calls.verify(listener).start(unsubscribe()); // merge's composite subscription
        // unnecessarily calls unsubscribe during the removing the subscription from the array.
        //
        // i didn't include it because it could cause a test failure if the internals change.
        calls.verify(listener, times(5)).complete(any()); // pop the call stack up to onNext(1)
        calls.verify(listener).start(onNext(3)); // from to map
        calls.verify(listener).start(onNext(Observable.class)); // map to merge
        calls.verify(listener).start(subscribe());
        calls.verify(listener).start(onNext(30)); // next from to merge inner
        calls.verify(listener).start(onNext(30)); // merge inner to take
        calls.verify(listener).start(onNext(30)); // take to output
        calls.verify(listener).complete(any());
        calls.verify(listener).start(onCompleted()); // take to output
        calls.verify(listener).start(unsubscribe()); // take unsubscribes
        calls.verify(listener).complete(any());
        calls.verify(listener).start(unsubscribe()); // merge inner unsubscribes
        calls.verify(listener).complete(any());
        calls.verify(listener).start(unsubscribe()); // merge outer unsubscribes
        calls.verify(listener).complete(any());
        calls.verify(listener).start(unsubscribe()); // map unsubscribe
        calls.verify(listener, times(7)).complete(any());
        calls.verifyNoMoreInteractions();

        listener.assertValidState();
    }

    private static <T> DebugNotification<T> onNext(final T value) {
        return argThat(new BaseMatcher<DebugNotification<T>>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof DebugNotification) {
                    @SuppressWarnings("unchecked")
                    DebugNotification<T> dn = (DebugNotification<T>) item;
                    return dn.getKind() == Kind.OnNext && dn.getValue().equals(value);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("OnNext " + value);
            }
        });
    }

    private static <T> DebugNotification<T> onNext(final Class<T> type) {
        return argThat(new BaseMatcher<DebugNotification<T>>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof DebugNotification) {
                    @SuppressWarnings("unchecked")
                    DebugNotification<T> dn = (DebugNotification<T>) item;
                    return dn.getKind() == Kind.OnNext && type.isAssignableFrom(dn.getValue().getClass());
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("OnNext " + type);
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private static <T> DebugNotification subscribe() {
        return argThat(new BaseMatcher<DebugNotification<T>>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof DebugNotification) {
                    DebugNotification dn = (DebugNotification) item;
                    return dn.getKind() == DebugNotification.Kind.Subscribe;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Subscribe");
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private static <T> DebugNotification unsubscribe() {
        return argThat(new BaseMatcher<DebugNotification<T>>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof DebugNotification) {
                    DebugNotification dn = (DebugNotification) item;
                    return dn.getKind() == DebugNotification.Kind.Unsubscribe;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Unsubscribe");
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private static DebugNotification onCompleted() {
        return argThat(new BaseMatcher<DebugNotification>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof DebugNotification) {
                    DebugNotification dn = (DebugNotification) item;
                    return dn.getKind() == Kind.OnCompleted;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("onCompleted");
            }
        });
    }
}

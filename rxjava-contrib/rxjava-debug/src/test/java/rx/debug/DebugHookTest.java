package rx.debug;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.plugins.DebugHook;
import rx.plugins.DebugNotification;
import rx.plugins.PlugReset;
import rx.plugins.RxJavaPlugins;

public class DebugHookTest {
    @Before
    @After
    public void reset() {
        PlugReset.reset();
    }

    @Test
    @Ignore
    public void testSimple() {
        Action1<DebugNotification> events = mock(Action1.class);
        final DebugHook hook = new DebugHook(null, events);
        RxJavaPlugins.getInstance().registerObservableExecutionHook(hook);
        Observable.empty().subscribe();
        verify(events, times(1)).call(subscribe());
        verify(events, times(1)).call(onCompleted());
    }

    @Test
    public void testOneOp() {
        Action1<DebugNotification> events = mock(Action1.class);
        final DebugHook hook = new DebugHook(null, events);
        RxJavaPlugins.getInstance().registerObservableExecutionHook(hook);
        Observable.from(Arrays.asList(1, 3)).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer it) {
                return Observable.from(Arrays.asList(it, it + 1));
            }
        }).take(3).subscribe(new Observer<Integer>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer t) {
            }
        });
        verify(events, times(6)).call(subscribe());
        verify(events, times(4)).call(onNext(1));
        // one less because it originates from the inner observable sent to merge
        verify(events, times(3)).call(onNext(2));
        verify(events, times(4)).call(onNext(3));
        // because the take unsubscribes
        verify(events, never()).call(onNext(4));
    }

    private static <T> DebugNotification<T> onNext(final T value) {
        return argThat(new BaseMatcher<DebugNotification<T>>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof DebugNotification) {
                    DebugNotification<T> dn = (DebugNotification<T>) item;
                    Notification<T> n = dn.getNotification();
                    return n != null && n.hasValue() && n.getValue().equals(value);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("OnNext " + value);
            }
        });
    }

    private static DebugNotification subscribe() {
        return argThat(new BaseMatcher<DebugNotification>() {
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

    private static DebugNotification onCompleted() {
        return argThat(new BaseMatcher<DebugNotification>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof DebugNotification) {
                    DebugNotification dn = (DebugNotification) item;
                    Notification n = dn.getNotification();
                    return n != null && n.isOnCompleted();
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

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.plugins.DebugHook;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotification.Kind;
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
        Func1 start = mock(Func1.class);
        Action1 complete = mock(Action1.class);
        Action2 error = mock(Action2.class);
        final DebugHook hook = new DebugHook(null, start, complete, error);
        RxJavaPlugins.getInstance().registerObservableExecutionHook(hook);
        Observable.empty().subscribe();
        verify(start, times(1)).call(subscribe());
        verify(start, times(1)).call(onCompleted());

        verify(complete, times(2)).call(any());

        verify(error, never()).call(any(), any());
    }

    @Test
    public void testOneOp() {
        Func1<DebugNotification<Integer, Object>, Object> start = mock(Func1.class);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object context = new Object();
                System.out.println("start: " + context.hashCode() + " " + invocation.getArguments()[0]);
                return context;
            }
        }).when(start).call(any(DebugNotification.class));
        Action1<Object> complete = mock(Action1.class);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.out.println("complete: " + invocation.getArguments()[0].hashCode());
                return null;
            }
        }).when(complete).call(any());
        Action2<Object, Throwable> error = mock(Action2.class);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.out.println("error: " + invocation.getArguments()[1].hashCode());
                return null;
            }
        }).when(error).call(any(), any(Throwable.class));
        final DebugHook hook = new DebugHook(null, start, complete, error);
        RxJavaPlugins.getInstance().registerObservableExecutionHook(hook);
        Observable.from(Arrays.asList(1, 3)).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer it) {
                return Observable.from(Arrays.asList(it, it + 1));
            }
        }).take(3).subscribe(new Subscriber<Integer>() {
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
        verify(start, atLeast(3)).call(subscribe());
        verify(start, times(4)).call(onNext(1));
        // one less because it originates from the inner observable sent to merge
        verify(start, times(3)).call(onNext(2));
        verify(start, times(4)).call(onNext(3));
        // because the take unsubscribes
        verify(start, never()).call(onNext(4));

        verify(complete, atLeast(14)).call(any());

        verify(error, never()).call(any(), any(Throwable.class));
    }

    private static <T, C> DebugNotification<T, C> onNext(final T value) {
        return argThat(new BaseMatcher<DebugNotification<T, C>>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof DebugNotification) {
                    DebugNotification<T, C> dn = (DebugNotification<T, C>) item;
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

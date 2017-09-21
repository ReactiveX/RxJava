package io.reactivex.internal.observers;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public final class CallbackCompletableObserverTest {

    @Test
    public void emptyActionShouldReportNoCustomOnError() {
        CallbackCompletableObserver o = new CallbackCompletableObserver(Functions.ON_ERROR_MISSING,
                Functions.EMPTY_ACTION,
                Functions.<Disposable>emptyConsumer());

        assertFalse(o.hasCustomOnError());
    }

    @Test
    public void customOnErrorShouldReportCustomOnError() {
        CallbackCompletableObserver o = new CallbackCompletableObserver(Functions.<Throwable>emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.<Disposable>emptyConsumer());

        assertTrue(o.hasCustomOnError());
    }

    @Test
    public void badSourceOnSubscribe() {
        Completable source = new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver s) {
                Disposable s1 = Disposables.empty();
                s.onSubscribe(s1);

                Disposable s2 = Disposables.empty();
                s.onSubscribe(s2);

                assertFalse(s1.isDisposed());
                assertTrue(s2.isDisposed());

                s.onComplete();
            }
        };

        final List<Object> received = new ArrayList<Object>();

        CallbackCompletableObserver o = new CallbackCompletableObserver(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {

            }
        }, new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                received.add(100);
            }
        });

        source.subscribe(o);

        assertEquals(Collections.singletonList(100), received);
    }

    @Test
    public void onSubscribeThrows() {
        final List<Object> received = new ArrayList<Object>();

        CallbackCompletableObserver o = new CallbackCompletableObserver(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {

            }
        }, new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                throw new TestException();
            }
        });


        assertFalse(o.isDisposed());

        Completable.complete().subscribe(o);

        assertTrue(received.toString(), received.get(0) instanceof TestException);

        assertTrue(o.isDisposed());
    }
}

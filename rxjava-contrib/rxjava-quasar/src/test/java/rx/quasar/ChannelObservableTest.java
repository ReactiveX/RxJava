/**
 * Copyright 2014 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.quasar;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.subjects.PublishSubject;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.ProducerException;
import co.paralleluniverse.strands.channels.ReceivePort;

public class ChannelObservableTest {
    @Test
    public void testObservableFromChannel() throws Exception {
        final Channel<String> c = Channels.newChannel(0);

        System.out.println("===== " + c);

        final Queue<String> result = new ConcurrentLinkedQueue<String>();
        final AtomicBoolean completed = new AtomicBoolean();

        ChannelObservable.from(c, NewFiberScheduler.getDefaultInstance()).subscribe(new Observer<String>() {
            @Override
            @Suspendable
            public void onNext(String t) {
                try {
                    System.out.println("GOT: " + t);
                    assertTrue(Strand.isCurrentFiber());
                    Strand.sleep(100);
                    result.add(t);
                } catch (InterruptedException e) {
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }

            @Override
            public void onCompleted() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {

            }
        });

        c.send("a");
        c.send("b");
        c.send("c");
        c.close();

        Thread.sleep(500);

        assertThat(new ArrayList<String>(result), equalTo(Arrays.asList("a", "b", "c")));
        assertThat(completed.get(), is(true));
    }

    @Test
    public void testObserverChannel() throws Exception {
        final Channel<String> c = Channels.newChannel(10); // must use a buffer, otherwise will block on subscribe

        System.out.println("===== " + c);

        Observable.from(Arrays.asList("a", "b", "c")).subscribe(ChannelObservable.to(c));

        assertThat(c.receive(), equalTo("a"));
        assertThat(c.receive(), equalTo("b"));
        assertThat(c.receive(), equalTo("c"));
        assertThat(c.receive(), is(nullValue()));
    }

    @Test
    public void testObserverChannel2() throws Exception {
        ReceivePort<String> c = ChannelObservable.subscribe(10, Channels.OverflowPolicy.BLOCK, Observable.from(Arrays.asList("a", "b", "c")));

        assertThat(c.receive(), equalTo("a"));
        assertThat(c.receive(), equalTo("b"));
        assertThat(c.receive(), equalTo("c"));
        assertThat(c.receive(), is(nullValue()));
    }

    @Test
    public void testObserverChannelWithError() throws Exception {
        PublishSubject<String> o = PublishSubject.create();
        ReceivePort<String> c = ChannelObservable.subscribe(10, Channels.OverflowPolicy.BLOCK, o);

        o.onNext("a");
        o.onError(new TestException());
        o.onNext("c");

        assertThat(c.receive(), equalTo("a"));
        try {
            c.receive();
            fail();
        } catch (ProducerException e) {
            assertThat(e.getCause(), instanceOf(TestException.class));
        }
        assertThat(c.isClosed(), is(true));
    }

    @Test
    public void whenGetThenBlockAndReturnResult() throws Exception {
        final PublishSubject<String> o = PublishSubject.create();

        Fiber<String> f = new Fiber<String>(new SuspendableCallable<String>() {

            @Override
            public String run() throws SuspendExecution, InterruptedException {
                try {
                    return ChannelObservable.get(o);
                } catch (ExecutionException e) {
                    throw new AssertionError();
                }
            }
        }).start();

        Thread.sleep(100);

        o.onNext("foo");
        o.onCompleted();

        assertThat(f.get(), equalTo("foo"));
    }

    @Test
    public void whenGetAndObservableFailsThenThrowExecutionException() throws Exception {
        final PublishSubject<String> o = PublishSubject.create();

        Fiber<String> f = new Fiber<String>(new SuspendableCallable<String>() {

            @Override
            public String run() throws SuspendExecution, InterruptedException {
                try {
                    return ChannelObservable.get(o);
                } catch (ExecutionException e) {
                    return e.getCause().getMessage();
                }
            }
        }).start();

        Thread.sleep(100);

        o.onError(new Exception("ohoh"));

        assertThat(f.get(), equalTo("ohoh"));
    }

    @Test
    public void whenGetAndObservableEmitsTwoValuesThenBlowup() throws Exception {
        final PublishSubject<String> o = PublishSubject.create();

        new Fiber<String>(new SuspendableCallable<String>() {

            @Override
            public String run() throws SuspendExecution, InterruptedException {
                try {
                    return ChannelObservable.get(o);
                } catch (ExecutionException e) {
                    throw new AssertionError();
                }
            }
        }).start();

        Thread.sleep(100);

        o.onNext("foo");
        try {
            o.onNext("bar");
            fail();
        } catch (Exception e) {
        }
    }
}

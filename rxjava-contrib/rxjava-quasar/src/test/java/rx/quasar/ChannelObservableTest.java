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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

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
        o.onError(new MyException());
        o.onNext("c");
        
        assertThat(c.receive(), equalTo("a"));
        try {
            c.receive();
            fail();
        } catch(MyException e) {
            
        }
        assertThat(c.receive(), is(nullValue()));
    }
    
    static class MyException extends RuntimeException {
        
    }
}

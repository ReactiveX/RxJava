/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import rx.Observable;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OperatorValveTest {
    @Test
    public void test() {
        // setup
        PublishSubject<Boolean> control = PublishSubject.create();
        OperatorValve<Integer> op = new OperatorValve<Integer>(control, 2);
        Subscriber<? super Integer> output = mock(Subscriber.class);
        Producer inputProducer = mock(Producer.class);
        InOrder order = inOrder(output, inputProducer);

        // invoke
        Subscriber<? super Integer> input = op.call(output);

        // verify
        // send in the input's producer
        input.setProducer(inputProducer);
        // capture the output's producer
        ArgumentCaptor<Producer> outputProducerCaptor = ArgumentCaptor.forClass(Producer.class);
        order.verify(output).setProducer(outputProducerCaptor.capture());
        Producer outputProducer = outputProducerCaptor.getValue();
        assertNotNull(outputProducer);

        // start sending request's and data
        int i = 0;

        // small request
        outputProducer.request(0);
        outputProducer.request(1);
        order.verify(inputProducer).request(1);
        input.onNext(++i);
        order.verify(output).onNext(i);

        // larger
        outputProducer.request(2);
        order.verify(inputProducer).request(2);
        input.onNext(++i);
        order.verify(output).onNext(i);
        input.onNext(++i);
        order.verify(output).onNext(i);

        // way too large
        outputProducer.request(5);
        order.verify(inputProducer).request(2);// request limited called by producer
        input.onNext(++i);
        order.verify(output).onNext(i);
        input.onNext(++i);
        order.verify(output).onNext(i);
        // next batch
        order.verify(inputProducer).request(2);// request limited called from onNext
        input.onNext(++i);
        order.verify(output).onNext(i);
        input.onNext(++i);
        order.verify(output).onNext(i);
        // next batch
        order.verify(inputProducer).request(1);
        control.onNext(false);
        input.onNext(++i);
        order.verify(output).onNext(i);

        // no request while closed 
        control.onNext(true);
        control.onNext(true);

        // small request while closed 
        control.onNext(false);
        outputProducer.request(1);
        control.onNext(true);
        order.verify(inputProducer).request(1);
        input.onNext(++i);
        order.verify(output).onNext(i);

        // larger request while closed 
        control.onNext(false);
        outputProducer.request(2);
        control.onNext(true);
        order.verify(inputProducer).request(2);
        input.onNext(++i);
        order.verify(output).onNext(i);
        input.onNext(++i);
        order.verify(output).onNext(i);

        // too large of a request while closed 
        control.onNext(false);
        outputProducer.request(3);
        control.onNext(true);
        order.verify(inputProducer).request(2);
        input.onNext(++i);
        order.verify(output).onNext(i);
        input.onNext(++i);
        order.verify(output).onNext(i);
        order.verify(inputProducer).request(1);
        input.onNext(++i);
        order.verify(output).onNext(i);

        input.onCompleted();
        order.verify(output).onCompleted();

        // all done
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testRequestError() {
        TestSubscriber<Void> tSub = TestSubscriber.create(-1);
        Exception e = new Exception();
        Observable.<Void> error(e).pressureValve(Observable.<Boolean> never(), 10).subscribe(tSub);
        tSub.assertError(e);
    }

    @Test
    public void testDataError() {
        TestSubscriber<Void> tSub = TestSubscriber.create();
        Exception e = new Exception();
        Observable.<Void> error(e).pressureValve(Observable.<Boolean> never(), 10).subscribe(tSub);
        tSub.assertError(e);
    }

    @Test
    public void testControlError() {
        TestSubscriber<Void> tSub = TestSubscriber.create();
        Exception e = new Exception();
        Observable.<Void> never().pressureValve(Observable.<Boolean> error(e), 10).subscribe(tSub);
        tSub.assertError(e);
    }

    @Test
    public void testControlCompleteOpen() {
        TestSubscriber<Integer> tSub = TestSubscriber.create();
        Observable.just(1, 2, 3).pressureValve(Observable.<Boolean> empty(), 10).subscribe(tSub);
        tSub.assertValues(1, 2, 3);
    }

    @Test
    public void testControlCompleteClosed() {
        TestSubscriber<Void> tSub = TestSubscriber.create();
        Observable.<Void> never().pressureValve(Observable.just(false), 10).subscribe(tSub);
        tSub.assertError(IllegalStateException.class);
    }

    /*
    @Test
    public void testObserveOn() {
        final AtomicLong counter = new AtomicLong();
        Observable<Integer> range = Observable.range(0, Integer.MAX_VALUE);
        Observable<Boolean> control = Observable.interval(1, TimeUnit.SECONDS).map(new Func1<Long, Boolean>() {
            @Override
            public Boolean call(Long i) {
                System.out.println();
                counter.set(0);
                return i % 2 == 1;
            }
        });
        long granularity = 10;
        TestSubscriber<Integer> tSub = new TestSubscriber<Integer>();
        range.pressureValve(control, granularity).observeOn(Schedulers.computation()).toBlocking().forEach(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                System.out.print(counter.incrementAndGet()+ "         \r");
            }
        });
    }
    
    public static void main(String[] args) {
        new OperatorValveTest().testObserveOn();
    }
    */
}

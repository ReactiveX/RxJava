/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.functions.*;

public class OperatorDoOnRequestTest {

    @Test
    public void testUnsubscribeHappensAgainstParent() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Observable.just(1)
        //
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        unsubscribed.set(true);
                    }
                })
                //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        // do nothing
                    }
                })
                //
                .subscribe();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void testDoRequest() {
        final List<Long> requests = new ArrayList<Long>();
        Observable.range(1, 5)
        //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        requests.add(n);
                    }
                })
                //
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(t);
                    }
                });
        assertEquals(Arrays.asList(3L,1L,2L,3L,4L,5L), requests);
    }
    
    @Test
    public void dontRequestIfDownstreamRequestsLate() {
        final List<Long> requested = new ArrayList<Long>();

        Action1<Long> empty = Actions.empty();
        
        final AtomicReference<Producer> producer = new AtomicReference<Producer>();
        
        Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t) {
                t.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        requested.add(n);
                    }
                });
            }
        }).doOnRequest(empty).subscribe(new Subscriber<Object>() {
            @Override
            public void onNext(Object t) {
                
            }
            
            @Override
            public void onError(Throwable e) {
                
            }
            
            @Override
            public void onCompleted() {
                
            }
            
            @Override
            public void setProducer(Producer p) {
                producer.set(p);
            }
        });
        
        producer.get().request(1);

        int s = requested.size();
        if (s == 1) {
            // this allows for an implementation that itself doesn't request
            Assert.assertEquals(Arrays.asList(1L), requested);
        } else {
            Assert.assertEquals(Arrays.asList(0L, 1L), requested);
        }
    }
}

/**
 * Copyright 2014 Netflix, Inc.
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

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * Emit ints from start to end inclusive.
 */
public final class OnSubscribeRange implements OnSubscribe<Integer> {

    private final int start;
    private final int end;

    public OnSubscribeRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void call(final Subscriber<? super Integer> o) {
        // TODO make a fast-path if site of Iterable is less than min buffer size so we avoid trampoline and producer in that case
        final Scheduler.Worker trampoline = Schedulers.trampoline().createWorker();
        o.add(trampoline);
        o.setProducer(new Producer() {
            // TODO migrate to AFU
            final AtomicInteger requested = new AtomicInteger();
            int index = start;

            @Override
            public void request(int n) {
                // TODO fast-path for n==-1 where it is infinite ... can skip the trampoline
                int _c = requested.getAndAdd(n);
                if (_c == 0) {
                    // it was 0 (not running) so start it
                    trampoline.schedule(new Action0() {

                        @Override
                        public void call() {
                            while (index <= end) {
                                if (o.isUnsubscribed()) {
                                    return;
                                }
                                o.onNext(index++);
                                int c = requested.decrementAndGet();
                                if (c == 0) {
                                    // we're done emitting the number requested so return
                                    return;
                                }
                            }
                            o.onCompleted();
                        }

                    });
                }

            }

        });

    }

}

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

import rx.Observable;
import rx.Producer;
import rx.Subscriber;

/**
 * Returns an Observable that skips the first <code>num</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/skip.png" alt="">
 * <p>
 * You can ignore the first <code>num</code> items emitted by an Observable and attend only to
 * those items that come after, by modifying the Observable with the {@code skip} operator.
 */
public final class OperatorSkip<T> implements Observable.Operator<T, T> {

    final int toSkip;

    public OperatorSkip(int n) {
        this.toSkip = n;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {

            int skipped = 0;

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                if (skipped >= toSkip) {
                    child.onNext(t);
                } else {
                    skipped += 1;
                }
            }

            @Override
            public void setProducer(final Producer producer) {
                child.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (n == Long.MAX_VALUE) {
                            // infinite so leave it alone
                            producer.request(n);
                        } else if (n > 0) {
                            // add the skip num to the requested amount, since we'll skip everything and then emit to the buffer downstream
                            producer.request(n + (toSkip - skipped));
                        }
                    }
                });
            }

        };
    }
}

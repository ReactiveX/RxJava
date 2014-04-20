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

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.channels.ReceivePort;

/**
 * Converts a {@link ReceivePort} into an Observable that emits each message received on the channel.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/toObservable.png">
 */
public final class OnSubscribeFromChannel<T> implements OnSubscribe<T> {

    final ReceivePort<? extends T> channel;

    public OnSubscribeFromChannel(ReceivePort<? extends T> channel) {
        this.channel = channel;
    }

    @Override
    @Suspendable
    public void call(Subscriber<? super T> o) {
        for (;;) {
            T m;

            try {
                m = channel.receive();
                if (m == null)
                    break;
                if (o.isUnsubscribed()) {
                    return;
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                o.onError(e);
                return;
            }

            o.onNext(m);
        }
        
        o.onCompleted();
    }
}

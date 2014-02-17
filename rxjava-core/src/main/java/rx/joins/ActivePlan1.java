/**
 * Copyright 2014 Netflix, Inc.
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
package rx.joins;

import rx.Notification;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Represents an active plan.
 */
public class ActivePlan1<T1> extends ActivePlan0 {
    private final Action1<T1> onNext;
    private final Action0 onCompleted;
    private final JoinObserver1<T1> first;

    public ActivePlan1(JoinObserver1<T1> first, Action1<T1> onNext, Action0 onCompleted) {
        this.onNext = onNext;
        this.onCompleted = onCompleted;
        this.first = first;
        addJoinObserver(first);
    }

    @Override
    public void match() {
        if (!first.queue().isEmpty()) {
            Notification<T1> n1 = first.queue().peek();
            if (n1.isOnCompleted()) {
                onCompleted.call();
            } else {
                dequeue();
                onNext.call(n1.getValue());
            }
        }
    }

}

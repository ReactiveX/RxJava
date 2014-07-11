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
import rx.functions.Action2;

/**
 * Represents an active plan.
 */
public final class ActivePlan2<T1, T2> extends ActivePlan0 {
    private final Action2<T1, T2> onNext;
    private final Action0 onCompleted;
    private final JoinObserver1<T1> jo1;
    private final JoinObserver1<T2> jo2;

    ActivePlan2(JoinObserver1<T1> jo1, JoinObserver1<T2> jo2, Action2<T1, T2> onNext, Action0 onCompleted) {
        this.onNext = onNext;
        this.onCompleted = onCompleted;
        this.jo1 = jo1;
        this.jo2 = jo2;
        addJoinObserver(jo1);
        addJoinObserver(jo2);
    }

    @Override
    protected void match() {
        if (!jo1.queue().isEmpty() && !jo2.queue().isEmpty()) {
            Notification<T1> n1 = jo1.queue().peek();
            Notification<T2> n2 = jo2.queue().peek();

            if (n1.isOnCompleted() || n2.isOnCompleted()) {
                onCompleted.call();
            } else {
                dequeue();
                onNext.call(n1.getValue(), n2.getValue());
            }
        }
    }

}

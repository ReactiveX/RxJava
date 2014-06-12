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
import rx.functions.Action5;

/**
 * Represents an active plan.
 */
public final class ActivePlan5<T1, T2, T3, T4, T5> extends ActivePlan0 {
    private final Action5<T1, T2, T3, T4, T5> onNext;
    private final Action0 onCompleted;
    private final JoinObserver1<T1> jo1;
    private final JoinObserver1<T2> jo2;
    private final JoinObserver1<T3> jo3;
    private final JoinObserver1<T4> jo4;
    private final JoinObserver1<T5> jo5;

    ActivePlan5(
    		JoinObserver1<T1> jo1,
            JoinObserver1<T2> jo2,
            JoinObserver1<T3> jo3,
            JoinObserver1<T4> jo4,
            JoinObserver1<T5> jo5,
            Action5<T1, T2, T3, T4, T5> onNext,
            Action0 onCompleted) {
        this.onNext = onNext;
        this.onCompleted = onCompleted;
        this.jo1 = jo1;
        this.jo2 = jo2;
        this.jo3 = jo3;
        this.jo4 = jo4;
        this.jo5 = jo5;
        addJoinObserver(jo1);
        addJoinObserver(jo2);
        addJoinObserver(jo3);
        addJoinObserver(jo4);
        addJoinObserver(jo5);
    }

    @Override
    protected void match() {
        if (!jo1.queue().isEmpty()
                && !jo2.queue().isEmpty()
                && !jo3.queue().isEmpty()
                && !jo4.queue().isEmpty()
                && !jo5.queue().isEmpty()
        ) {
            Notification<T1> n1 = jo1.queue().peek();
            Notification<T2> n2 = jo2.queue().peek();
            Notification<T3> n3 = jo3.queue().peek();
            Notification<T4> n4 = jo4.queue().peek();
            Notification<T5> n5 = jo5.queue().peek();

            if (n1.isOnCompleted() 
            		|| n2.isOnCompleted() 
            		|| n3.isOnCompleted()
            		|| n4.isOnCompleted()
            		|| n5.isOnCompleted()
    		) {
                onCompleted.call();
            } else {
                dequeue();
                onNext.call(
                		n1.getValue(), 
                		n2.getValue(), 
                		n3.getValue(), 
                		n4.getValue(),
                		n5.getValue()
        		);
            }
        }
    }

}

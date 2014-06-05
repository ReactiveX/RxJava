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
import rx.functions.Action9;

/**
 * Represents an active plan.
 */
public final class ActivePlan9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends ActivePlan0 {
    private final Action9<T1, T2, T3, T4, T5, T6, T7, T8, T9> onNext;
    private final Action0 onCompleted;
    private final JoinObserver1<T1> jo1;
    private final JoinObserver1<T2> jo2;
    private final JoinObserver1<T3> jo3;
    private final JoinObserver1<T4> jo4;
    private final JoinObserver1<T5> jo5;
    private final JoinObserver1<T6> jo6;
    private final JoinObserver1<T7> jo7;
    private final JoinObserver1<T8> jo8;
    private final JoinObserver1<T9> jo9;

    ActivePlan9(
    		JoinObserver1<T1> jo1,
            JoinObserver1<T2> jo2,
            JoinObserver1<T3> jo3,
            JoinObserver1<T4> jo4,
            JoinObserver1<T5> jo5,
            JoinObserver1<T6> jo6,
            JoinObserver1<T7> jo7,
            JoinObserver1<T8> jo8,
            JoinObserver1<T9> jo9,
            Action9<T1, T2, T3, T4, T5, T6, T7, T8, T9> onNext,
            Action0 onCompleted) {
        this.onNext = onNext;
        this.onCompleted = onCompleted;
        this.jo1 = jo1;
        this.jo2 = jo2;
        this.jo3 = jo3;
        this.jo4 = jo4;
        this.jo5 = jo5;
        this.jo6 = jo6;
        this.jo7 = jo7;
        this.jo8 = jo8;
        this.jo9 = jo9;
        addJoinObserver(jo1);
        addJoinObserver(jo2);
        addJoinObserver(jo3);
        addJoinObserver(jo4);
        addJoinObserver(jo5);
        addJoinObserver(jo6);
        addJoinObserver(jo7);
        addJoinObserver(jo8);
        addJoinObserver(jo9);
    }

    @Override
    protected void match() {
        if (!jo1.queue().isEmpty()
                && !jo2.queue().isEmpty()
                && !jo3.queue().isEmpty()
                && !jo4.queue().isEmpty()
                && !jo5.queue().isEmpty()
                && !jo6.queue().isEmpty()
                && !jo7.queue().isEmpty()
                && !jo8.queue().isEmpty()
                && !jo9.queue().isEmpty()
        ) {
            Notification<T1> n1 = jo1.queue().peek();
            Notification<T2> n2 = jo2.queue().peek();
            Notification<T3> n3 = jo3.queue().peek();
            Notification<T4> n4 = jo4.queue().peek();
            Notification<T5> n5 = jo5.queue().peek();
            Notification<T6> n6 = jo6.queue().peek();
            Notification<T7> n7 = jo7.queue().peek();
            Notification<T8> n8 = jo8.queue().peek();
            Notification<T9> n9 = jo9.queue().peek();

            if (n1.isOnCompleted() 
            		|| n2.isOnCompleted() 
            		|| n3.isOnCompleted()
            		|| n4.isOnCompleted()
            		|| n5.isOnCompleted()
            		|| n6.isOnCompleted()
            		|| n7.isOnCompleted()
            		|| n8.isOnCompleted()
            		|| n9.isOnCompleted()
    		) {
                onCompleted.call();
            } else {
                dequeue();
                onNext.call(
                		n1.getValue(), 
                		n2.getValue(), 
                		n3.getValue(), 
                		n4.getValue(),
                		n5.getValue(),
                		n6.getValue(),
                		n7.getValue(),
                		n8.getValue(),
                		n9.getValue()
        		);
            }
        }
    }

}

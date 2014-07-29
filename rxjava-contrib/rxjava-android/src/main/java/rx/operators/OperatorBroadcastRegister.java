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
package rx.operators;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class OperatorBroadcastRegister implements Observable.OnSubscribe<Intent> {

    private final Context context;
    private final IntentFilter intentFilter;
    private final String broadcastPermission;
    private final Handler schedulerHandler;

    public OperatorBroadcastRegister(Context context, IntentFilter intentFilter, String broadcastPermission, Handler schedulerHandler) {
        this.context = context;
        this.intentFilter = intentFilter;
        this.broadcastPermission = broadcastPermission;
        this.schedulerHandler = schedulerHandler;
    }

    @Override
    public void call(final Subscriber<? super Intent> subscriber) {
        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                subscriber.onNext(intent);
            }
        };

        final Subscription subscription = Subscriptions.create(new Action0() {
            @Override
            public void call() {
                context.unregisterReceiver(broadcastReceiver);
            }
        });

        subscriber.add(subscription);
        context.registerReceiver(broadcastReceiver, intentFilter, broadcastPermission, schedulerHandler);

    }
}

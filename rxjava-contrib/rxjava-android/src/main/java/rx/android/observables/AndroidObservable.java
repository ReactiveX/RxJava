/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.android.observables;

import android.hardware.SensorManager;
import rx.Observable;
import rx.operators.OperationObserveFromAndroidComponent;

import android.app.Activity;
import android.app.Fragment;
import rx.operators.OperationObserveFromAndroidSensor;

public final class AndroidObservable {

    private AndroidObservable() {}

    public static <T> Observable<T> fromActivity(Activity activity, Observable<T> sourceObservable) {
        return OperationObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, activity);
    }

    public static <T> Observable<T> fromFragment(Fragment fragment, Observable<T> sourceObservable) {
        return OperationObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, fragment);
    }

    public static <T> Observable<T> fromFragment(android.support.v4.app.Fragment fragment, Observable<T> sourceObservable) {
        return OperationObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, fragment);
    }

    public static Observable<float[]> fromSensor(SensorManager sensorManager, int type, int rate) {
        return  OperationObserveFromAndroidSensor.observeFromAndroidSensor(sensorManager, type, rate);
    }

}

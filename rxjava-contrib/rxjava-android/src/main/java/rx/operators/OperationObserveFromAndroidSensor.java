package rx.operators;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import rx.Observable;
import rx.subjects.PublishSubject;

public class OperationObserveFromAndroidSensor {
    public static Observable<float[]> observeFromAndroidSensor(SensorManager sensorManager, int type, int rate) {
        Sensor sensor = sensorManager.getDefaultSensor(type);

        if (sensor == null) throw new IllegalArgumentException("Unsupported sensor type.");

        final PublishSubject<float[]> subject = PublishSubject.create();
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                subject.onNext(event.values);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }
        };

        sensorManager.registerListener(listener, sensor, rate);

        return subject;
    }
}

package com.netflix.rxjava.android.samples;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;

public class SampleFragmentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.enableDefaults();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_activity);
    }

}

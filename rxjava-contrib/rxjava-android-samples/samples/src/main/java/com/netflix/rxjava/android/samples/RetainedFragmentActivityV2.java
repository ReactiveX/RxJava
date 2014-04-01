package com.netflix.rxjava.android.samples;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * A retained fragment whose goals are below
 *
 * 1) gracefully handle rotation - not losing any data
 * 2) gracefully handle the user moving in and out of the app
 * 3) use a button or trigger of some sort to start the observable, something more in line with typical use
 * 4) ensure that the callbacks are not called if the user moves away from the fragment
 *
 * @author zsiegel (zsiegel87@gmail.com)
 */
public class RetainedFragmentActivityV2 extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setTitle("Fake API call V2");
        setContentView(R.layout.retained_fragment_activity_v2);
    }

    @SuppressWarnings("ConstantConditions")
    public static class RetainedFragmentV2 extends Fragment {

        private Observable<String> observable;
        private Subscription subscription = Subscriptions.empty();
        private Button startButton;
        private boolean progressVisiblity;

        // in a production app, you don't want to have JSON parser code in your fragment,
        // but we'll simplify a little here
        private static final Func1<String, String> PARSE_JSON = new Func1<String, String>() {
            @Override
            public String call(String json) {
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    return String.valueOf(jsonObject.getInt("result"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        public RetainedFragmentV2() {
            setRetainInstance(true);
        }

        /**
         * We un-subscribe whenever we are paused
         */
        @Override
        public void onPause() {
            subscription.unsubscribe();
            super.onPause();
        }

        /**
         * We re-subscribe whenever we are resumed
         */
        @Override
        public void onResume() {
            super.onResume();
            subscribe();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.retained_fragment_v2, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            final TextView textView = (TextView)getView().findViewById(android.R.id.text1);

            startButton = (Button) view.findViewById(R.id.button);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textView.setText("");
                    start();
                    startButton.setEnabled(false);
                }
            });
        }

        private void start() {

            progressVisiblity = true;

            observable = SampleObservables
                    .fakeApiCall(5000)
                    .map(PARSE_JSON)
                    .observeOn(AndroidSchedulers.mainThread())
                    .cache();

            subscribe();
        }

        /**
         * We subscribe/re-subscribe here
         */
        private void subscribe() {
            if (observable != null) {

                getActivity().setProgressBarIndeterminateVisibility(progressVisiblity);

                final TextView textView = (TextView)getView().findViewById(android.R.id.text1);

                subscription = observable.subscribe(new Action1<String>() {
                    @Override
                    public void call(String result) {
                        textView.setText(result);
                        progressVisiblity = false;
                        getActivity().setProgressBarIndeterminateVisibility(progressVisiblity);
                        startButton.setEnabled(true);
                    }
                });
            }
        }
    }
}

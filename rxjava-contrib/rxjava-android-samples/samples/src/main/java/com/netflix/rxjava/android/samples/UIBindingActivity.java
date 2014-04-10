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
 * Problem:
 * You have a data source (where that data is potentially expensive to obtain), and you want to
 * emit this data into a fragment. However, you want to gracefully deal with rotation changes and
 * not lose any data already emitted.
 * <p/>
 * You also want your UI to update accordingly to the data being emitted.
 *
 * @author zsiegel (zsiegel87@gmail.com)
 */
public class UIBindingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setTitle("UIBinding");
        setContentView(R.layout.ui_binding_activity);
    }

    @SuppressWarnings("ConstantConditions")
    public static class RetainedBindingFragment extends Fragment {

        private Button startButton;

        private Observable<String> request;
        private Subscription requestSubscription = Subscriptions.empty();

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

        public RetainedBindingFragment() {
            setRetainInstance(true);
        }

        /**
         * We un-subscribe whenever we are paused
         */
        @Override
        public void onPause() {
            requestSubscription.unsubscribe();
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

            final TextView textView = (TextView) getView().findViewById(android.R.id.text1);

            startButton = (Button) view.findViewById(R.id.button);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textView.setText("");
                    start();
                }
            });
        }

        private void start() {

            request = SampleObservables
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
            if (request != null) {

                final TextView textView = (TextView) getView().findViewById(android.R.id.text1);

                requestSubscription = request.map(new Func1<String, Boolean>() {

                    //Consume the data that comes back and then signal that we are done
                    @Override
                    public Boolean call(String s) {
                        textView.setText(s);
                        return true;
                    }
                }).startWith(false) //Before we receive data our request is not complete
                  .subscribe(new Action1<Boolean>() {

                    //We update the UI based on the state of the request
                    @Override
                    public void call(Boolean completed) {
                        setRequestInProgress(completed);
                    }
                });
            }
        }

        private void setRequestInProgress(boolean completed) {
            getActivity().setProgressBarIndeterminateVisibility(!completed);
            startButton.setEnabled(completed);
        }
    }
}

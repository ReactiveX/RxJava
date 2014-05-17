package com.netflix.rxjava.android.samples;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import static rx.android.observables.AndroidObservable.bindFragment;

/**
 * Problem:
 * You have a data source (where that data is potentially expensive to obtain), and you want to
 * emit this data into a fragment. However, you want to gracefully deal with rotation changes and
 * not lose any data already emitted.
 * <p/>
 * Solution:
 * Combine {@link android.app.Fragment#setRetainInstance(boolean)} with
 * {@link rx.android.schedulers.AndroidSchedulers#mainThread()} and {@link rx.Observable#cache()}
 */
public class RetainedFragmentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setTitle("Fake API call");
        setContentView(R.layout.retained_fragment_activity);
    }

    @SuppressWarnings("ConstantConditions")
    public static class RetainedFragment extends Fragment {

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

        private Observable<String> strings;
        private Subscription subscription = Subscriptions.empty();

        public RetainedFragment() {
            setRetainInstance(true);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // simulate fetching a JSON document with a latency of 2 seconds
            // in retained fragments, it's sufficient to bind the fragment in onCreate, since
            // Android takes care of detaching the Activity for us, and holding a reference for
            // the duration of the observable does not harm.
            strings = bindFragment(this, SampleObservables.fakeApiCall(2000).map(PARSE_JSON).cache());
        }

        @Override
        public void onDestroyView() {
            subscription.unsubscribe();
            super.onDestroyView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            getActivity().setProgressBarIndeterminateVisibility(true);
            return inflater.inflate(R.layout.retained_fragment, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            final TextView textView = (TextView) view.findViewById(android.R.id.text1);

            // (re-)subscribe to the sequence, which either emits the cached result or simply re-
            // attaches the subscriber to wait for it to arrive
            subscription = strings.subscribe(new Action1<String>() {
                @Override
                public void call(String result) {
                    textView.setText(result);
                    getActivity().setProgressBarIndeterminateVisibility(false);
                }
            });
        }
    }
}

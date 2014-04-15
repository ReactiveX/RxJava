package com.netflix.rxjava.android.samples;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import rx.Subscriber;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import static rx.android.observables.AndroidObservable.bindFragment;

/**
 * Problem:
 * You have a background sequence which keeps emitting items (either a limited or unlimited number)
 * and your UI component should be able to "listen in" to the sequence, i.e. it's okay to miss
 * in-flight items when going e.g. through a screen rotation or being otherwise detached from the
 * screen for a limited period of time. (Another example is a "page out" in a fragment ViewPager.)
 * <p/>
 * This is useful if you need behavior that mimics event buses. Think of a publishing
 * Observable as a channel or queue on an event bus.
 * <p/>
 * Solution:
 * Combine {@link android.app.Fragment#setRetainInstance(boolean)} with
 * {@link rx.android.schedulers.AndroidSchedulers#mainThread()} and {@link rx.Observable#publish()}
 */
public class ListeningFragmentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listening_fragment_activity);
    }

    @SuppressWarnings("ConstantConditions")
    public static class ListeningFragment extends Fragment {

        private ConnectableObservable<String> strings;
        private Subscription subscription = Subscriptions.empty();

        public ListeningFragment() {
            setRetainInstance(true);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            strings = SampleObservables.numberStrings(1, 50, 250).publish();
            strings.connect(); // trigger the sequence
        }

        @Override
        public void onDestroyView() {
            subscription.unsubscribe(); // stop listening
            super.onDestroyView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.retained_fragment, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            final TextView textView = (TextView) view.findViewById(android.R.id.text1);

            // re-connect to sequence
            subscription = bindFragment(this, strings).subscribe(new Subscriber<String>() {

                @Override
                public void onCompleted() {
                    Toast.makeText(getActivity(), "Done!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onNext(String s) {
                    textView.setText(s);
                }
            });
        }
    }
}

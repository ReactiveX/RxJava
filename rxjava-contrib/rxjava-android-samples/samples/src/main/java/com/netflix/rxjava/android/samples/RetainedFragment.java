package com.netflix.rxjava.android.samples;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import rx.Observable;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

/**
 * This fragment shows one of the most common use cases: listening for data emitted from a background
 * task on the UI.
 * <p/>
 * We achieve this by retaining the fragment, creating the observable sequence in onCreate (thus
 * also retaining the sequence), and connecting to it in onViewCreated. Note how we use the cache
 * operator to replay items already emitted. This ensure that if we should go through a configuration
 * change, and the activity gets destroyed, we do not lose any data, but simply cache and re-emit
 * it when the views get recreated.
 */
public class RetainedFragment extends Fragment {

    private Observable<String> strings;
    private Subscription subscription = Subscriptions.empty();

    public RetainedFragment() {
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        strings = SampleObservables.numberStrings2().cache();
    }

    @Override
    public void onDestroyView() {
        subscription.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.retained_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subscription = AndroidObservable.fromFragment(this, strings).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                final TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setText(s);
            }
        });
    }
}

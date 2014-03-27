package com.netflix.rxjava.android.samples;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import rx.Observable;
import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

/**
 * Problem:
 * You have an asynchronous sequence that emits items to be displayed in a list. You want the data
 * to survive rotation changes.
 * <p/>
 * Solution:
 * Combine {@link android.app.Fragment#setRetainInstance(boolean)} in a ListFragment with
 * {@link rx.android.schedulers.AndroidSchedulers#mainThread()} and an {@link rx.Observable.Operator}
 * that binds to the list adapter.
 */
public class ListFragmentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Lists");
        setContentView(R.layout.list_fragment_activity);
    }

    @SuppressWarnings("ConstantConditions")
    public static class RetainedListFragment extends ListFragment {

        private ArrayAdapter<String> adapter;

        public RetainedListFragment() {
            setRetainInstance(true);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
            setListAdapter(adapter);
            SampleObservables.numberStrings(1, 20, 250)
                    .observeOn(mainThread())
                    .lift(new BindAdapter())
                    .subscribe();
        }

        private final class BindAdapter implements Observable.Operator<String, String> {
            @Override
            public Subscriber<? super String> call(Subscriber<? super String> subscriber) {
                return new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onNext(String strings) {
                        adapter.add(strings);
                    }
                };
            }
        }
    }
}

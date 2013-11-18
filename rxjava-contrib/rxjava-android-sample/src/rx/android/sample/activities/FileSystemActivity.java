package rx.android.sample.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.android.observables.AndroidObservable;
import rx.android.sample.R;
import rx.android.sample.util.LogUtil;
import rx.android.sample.util.ThreadUtils;
import rx.concurrency.Schedulers;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.Subscriptions;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Based on: http://www.codeproject.com/Articles/52308/The-Rx-Framework-By-Example
 */
public class FileSystemActivity extends Activity {
	private static final String TAG = FileSystemActivity.class.getSimpleName();

	private Button cancelButton;
	private ListView listView;

	private SafeObservableSubscription subscription;
	private BaseAdapter adapter;

	private final List<String> data = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		adapter = new DataListAdapter();
		listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(adapter);
		listView.setFastScrollEnabled(true);

		cancelButton = (Button) findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				subscription.unsubscribe();
				cancelButton.setEnabled(false);
			}
		});

		Observable<String> filesObservable = Observable
			.create(externalStorageFileFunc)
			.subscribeOn(Schedulers.threadPoolForComputation());

		subscription = (SafeObservableSubscription) AndroidObservable
			.fromActivity(this, filesObservable)
			.buffer(500, TimeUnit.MILLISECONDS)
			.observeOn(AndroidSchedulers.mainThread()) // Current thread by default but we're being explicit.
			.subscribe(new Observer<List<String>>() {
				@Override
				public void onNext(List<String> values) {
					ThreadUtils.assertOnMain();
					Log.v(TAG, "onNext, " + values.size() + " items");
					data.addAll(values);
					adapter.notifyDataSetChanged();
				}

				@Override
				public void onCompleted() {
					ThreadUtils.assertOnMain();
					Log.v(TAG, "onCompleted");
					cancelButton.setEnabled(false);
				}

				@Override
				public void onError(Throwable e) {
					ThreadUtils.assertOnMain();
					LogUtil.handleException(TAG, e);
					cancelButton.setEnabled(false);
				}
			});
	}

	private final OnSubscribeFunc<String> externalStorageFileFunc = new OnSubscribeFunc<String>() {
		@Override
		public Subscription onSubscribe(Observer<? super String> observer) {
			ThreadUtils.assertNotOnMain();

			getAllFiles(observer, Environment.getExternalStorageDirectory());
			observer.onCompleted();
			return Subscriptions.empty();
		}

		private void getAllFiles(Observer<? super String> observer, File candidateDir) {
			if ((subscription != null) && subscription.isUnsubscribed()) {
				return;
			}

			File[] files = candidateDir.listFiles();
			if (files == null) {
				observer.onNext(candidateDir.getAbsolutePath());

				try {
					Thread.sleep(10); // Fake delay to simulate slower operation
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				for (File f : files) {
					getAllFiles(observer, f);
				}
			}
		}
	};

	private class DataListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public String getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new TextView(FileSystemActivity.this);
			}
			((TextView)convertView).setText(getItem(position));
			return convertView;
		}
	}
}

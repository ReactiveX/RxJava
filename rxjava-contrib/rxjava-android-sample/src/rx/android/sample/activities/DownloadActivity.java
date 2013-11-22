package rx.android.sample.activities;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.android.observables.AndroidObservable;
import rx.android.sample.util.IOUtils;
import rx.android.sample.util.LogUtil;
import rx.android.sample.util.ThreadUtils;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class DownloadActivity extends Activity {
	protected static final String TAG = DownloadActivity.class.getSimpleName();

	private TextView tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int p = 48;

		tv = new TextView(this);
		tv.setPadding(p, p, p, p);
		tv.setText("Tap here to start download");
		tv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tv.setEnabled(false);
				AndroidObservable
					.fromActivity(
						DownloadActivity.this,
						createDownloadObservable().subscribeOn(Schedulers.threadPoolForIO()))
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(new Observer<String>() {
						@Override
						public void onNext(String val) {
							Log.v(TAG, "onNext: " + val);
						}

						@Override
						public void onCompleted() {
							Log.v(TAG, "onCompleted");
							tv.setEnabled(true);
						}

						@Override
						public void onError(Throwable e) {
							LogUtil.handleException(TAG, e);
							tv.setEnabled(true);
						}
					});
			}
		});

		setContentView(tv);
	}

	public Observable<String> createDownloadObservable() {
	    return Observable.create(new OnSubscribeFunc<String>() {
	        @Override
	        public Subscription onSubscribe(Observer<? super String> observer) {
	        	ThreadUtils.assertNotOnMain();
	            try {
	                observer.onNext(IOUtils.fetchResponse("http://en.wikipedia.org/wiki/tiger"));
	                observer.onCompleted();
	            } catch (Exception e) {
	                observer.onError(e);
	            }
	            return Subscriptions.empty();
	        }
	    });
	}
}

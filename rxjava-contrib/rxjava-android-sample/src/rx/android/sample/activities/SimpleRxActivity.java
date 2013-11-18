package rx.android.sample.activities;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.android.sample.model.Observers.LoggingObserver;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class SimpleRxActivity extends Activity {
	private static final String TAG = SimpleRxActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TextView tv = new TextView(this);
		int p = 16;
		tv.setPadding(p, p, p, p);
		tv.setText("See logcat for output, tag: " + TAG);
		setContentView(tv);

		runSimpleIntObserver();
		runSimpleStringObserver();
	}

	private final OnSubscribeFunc<Integer> intGeneraterFunc = new OnSubscribeFunc<Integer>() {
		@Override
		public Subscription onSubscribe(final Observer<? super Integer> observer) {
			Log.v(TAG, getThreadName() + "onSubscribe()");
			try {
				int i;
				for (i = 0; i < 3; i++) {
					observer.onNext(i);
				}

				if (i > 0) {
					throw new IllegalStateException("Dummy error");
				}

				observer.onCompleted();
			}
			catch(Exception e) {
				observer.onError(e);
			}

            return Subscriptions.empty();
		}
	};

	private void runSimpleIntObserver() {
		Observable
			.create(intGeneraterFunc)
			.map(new Func1<Integer, Integer>() {
				@Override
				public Integer call(Integer i) {
					Log.v(TAG, getThreadName() + "map call, input: " + i);
					return i * 2;
				}
			})
			.subscribeOn(Schedulers.newThread())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(new LoggingObserver<Integer>(TAG));
	}

	private void runSimpleStringObserver() {
		String[] names = {"alpha", "beta", "charlie"};
		Observable
			.from(names)
			.subscribe(new Observer<String>() {
				@Override
				public void onNext(String s) {
					Log.v(TAG, getThreadName() + "Hello " + s);
				}

				@Override
				public void onCompleted() {
					Log.v(TAG, getThreadName() + "Complete");
				}

				@Override
				public void onError(Throwable e) {
					Log.v(TAG, getThreadName() + "Error: " + e);
				}
			});
	}

	private String getThreadName() {
		return "[" + Thread.currentThread().getName() + "]: ";
	}
}

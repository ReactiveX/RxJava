package rx.android.sample.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class MainActivity extends Activity {
//	private static final String TAG = MainActivity.class.getSimpleName();

	private final List<Intent> launchIntents = new ArrayList<Intent>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		launchIntents.add(new Intent(this, SimpleRxActivity.class));
		launchIntents.add(new Intent(this, FileSystemActivity.class));
		launchIntents.add(new Intent(this, DownloadActivity.class));

		GridView gv = new GridView(this);
		gv.setNumColumns(2);
		gv.setAdapter(new ActivityLaunchGridAdapter());
		gv.setOnItemClickListener((OnItemClickListener) gv.getAdapter());
		setContentView(gv);
	}

	private class ActivityLaunchGridAdapter extends BaseAdapter implements OnItemClickListener {

		@Override
		public int getCount() {
			return launchIntents.size();
		}

		@Override
		public Object getItem(int position) {
			return launchIntents.get(position);
		}

		private CharSequence getItemAsString(int position) {
			String className = ((Intent)getItem(position)).getComponent().getClassName();
			int i = className.lastIndexOf('.');
			return className.substring(i + 1);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = new TextView(MainActivity.this);
			tv.setText(getItemAsString(position));
			tv.setTextSize(16);
			int p = 48;
			tv.setPadding(p, p, p, p);
			return tv;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			startActivity((Intent)getItem(position));
		}
	}
}

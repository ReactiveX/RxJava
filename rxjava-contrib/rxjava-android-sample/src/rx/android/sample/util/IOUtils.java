package rx.android.sample.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class IOUtils {
	private static final String TAG = IOUtils.class.getSimpleName();

	public static String createStringFromStream(InputStream inputStream) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder total = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
		    total.append(line);
		}
		return total.toString();
	}

	public static String fetchResponse(String urlString) {
		URL url;
		HttpURLConnection urlConnection = null;
		Log.v(TAG, "Starting Http connection...");
		try {
			url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			return IOUtils.createStringFromStream(urlConnection.getInputStream());
		}
		catch (Exception e) {
			LogUtil.handleException(TAG, e);
			return null;
		}
		finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			Log.v(TAG, "Http connection closed");
		}
	}
}

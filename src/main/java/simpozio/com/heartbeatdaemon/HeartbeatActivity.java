package simpozio.com.heartbeatdaemon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class HeartbeatActivity extends Activity {

    private final Thread heartbeat = new HeartbeatRunner();

    @Override
    protected void onCreate(Bundle sis) {
        super.onCreate(sis);
        this.setContentView(R.layout.activity_main);
        this.heartbeat.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.heartbeat.interrupt();
    }

    private class HeartbeatRunner extends Thread {

        private static final long HEARTBEAT_PERIOD_IN_MILLIS = 3000;

        private final String LOG_TAG = HeartbeatRunner.class.getName();

        private Headers headers = Headers.of(rawHeaders());

        private String testEnvUrl = "https://api-test.simpozio.com/v2/signals/heartbeat";

        private DateFormat dateFormatter = new SimpleDateFormat("\"yyyy-MM-dd'T'HH:mm:ss.SSSZ\"");

        private MediaType mediaType = MediaType.parse("application/json");

        private final OkHttpClient client = new OkHttpClient();

        @Override
        @SuppressLint("WakelockTimeout")
        public void run() {

            PowerManager.WakeLock wakeLock = ((PowerManager) HeartbeatActivity.this.getSystemService(POWER_SERVICE)).newWakeLock(PARTIAL_WAKE_LOCK, "hbwl");

            if (wakeLock != null) {
                try {
                    wakeLock.acquire();
                    while (!Thread.interrupted()) {
                        try {
                            this.client.newCall(testHeartbeatRequest()).execute().close();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Request sending error - " + e.getMessage());
                        }
                        try {
                            Thread.sleep(HEARTBEAT_PERIOD_IN_MILLIS);
                        } catch (InterruptedException ignored) {
                            this.interrupt();
                        }
                    }
                } finally {
                    wakeLock.release();
                }
            } else {
                Log.e(LOG_TAG, "WakeLock is null");
            }
        }

        private Request testHeartbeatRequest() {
            return new Request.Builder()
                    .url(testEnvUrl)
                    .post(body())
                    .headers(headers)
                    .build();
        }

        private RequestBody body() {
            return RequestBody.create(mediaType, testContent());
        }

        private String testContent() {
            return new StringBuilder("{")
                    .append("\"timestamp\":").append(dateFormatter.format(Calendar.getInstance().getTime())).append(",")
                    .append("\"touchpoint\":\"touchpoint_test\"").append(",")
                    .append("\"url\":\"url_test_1\"").append(",")
                    .append("\"connection\":\"connection_test\"").append(",")
                    .append("\"bandwidth\":\"bandwidth_test\"").append(",")
                    .append("\"payload\":\"payload_test\"")
                    .append("}").toString();
        }

        public Map<String, String> rawHeaders() {
            return new HashMap<String, String>() {{
                // FIXME: hardcode
                this.put("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MjU5NTY3MjUsImV4cGlyZXMiOjE1NTE4NzY3MjV9.eX8NIOOTRBCLxw3bR6L94fh7g_h527vUQ_BQ16DdJaM");
                this.put("Cookie", "__cfduid=d774a81b95e75d1c9d776bdeff4aa10b21522752584");
            }};
        }
    }
}
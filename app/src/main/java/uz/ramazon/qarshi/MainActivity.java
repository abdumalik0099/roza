package uz.ramazon.qarshi;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;
    private int notifId = 1000;
    static final String PREFS = "namoz_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.parseColor("#06191a"));
            getWindow().setNavigationBarColor(Color.parseColor("#071f1f"));
        }

        // Notification ruxsati so'rash (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        // Foreground Service ishga tushirish
        try {
            Intent svc = new Intent(this, PrayerService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
        } catch (Exception e) { e.printStackTrace(); }

        // WebView
        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#06191a"));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);

        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return false; }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest r) { r.grant(r.getResources()); }
        });
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    public class Bridge {
        @JavascriptInterface public boolean isAndroid() { return true; }

        // Namoz bildirganomasi
        @JavascriptInterface
        public void showNotification(String title, String body) {
            postNotif(title, body, AlarmReceiver.CH_PRAYER, Color.parseColor("#17857e"), notifId++);
        }

        // Ramazon bildirganomasi
        @JavascriptInterface
        public void showRamazonNotification(String title, String body) {
            postNotif(title, body, AlarmReceiver.CH_RAMAZON, Color.parseColor("#f0cc70"), notifId++);
        }

        // Sozlamani saqlash (notification on/off)
        @JavascriptInterface
        public void saveSetting(String key, boolean value) {
            SharedPreferences.Editor ed = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            ed.putBoolean(key, value).apply();
            // Alarmlarni qayta rejalashtirish
            PrayerScheduler.scheduleAll(MainActivity.this);
        }

        // Sozlamani o'qish
        @JavascriptInterface
        public boolean getSetting(String key) {
            return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(key, true);
        }

        // Barcha sozlamalar
        @JavascriptInterface
        public String getAllSettings() {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            String[] keys = {"notif_bomdod","notif_quyosh","notif_peshin","notif_asr",
                             "notif_shom","notif_xufton","notif_sahar30","notif_sahar10",
                             "notif_iftor15","notif_iftor5"};
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < keys.length; i++) {
                sb.append("\"").append(keys[i]).append("\":").append(p.getBoolean(keys[i], true));
                if (i < keys.length-1) sb.append(",");
            }
            sb.append("}");
            return sb.toString();
        }

        @JavascriptInterface
        public boolean hasNotificationPermission() {
            if (Build.VERSION.SDK_INT >= 33)
                return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                       == PackageManager.PERMISSION_GRANTED;
            return true;
        }
    }

    void postNotif(String title, String body, String channel, int color, int id) {
        try {
            AlarmReceiver.createChannels(this);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int pf = Build.VERSION.SDK_INT >= 23
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, pf);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, channel)
                : new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_dialog_info)
             .setContentTitle(title).setContentText(body)
             .setContentIntent(pi).setAutoCancel(true);
            if (Build.VERSION.SDK_INT >= 21) b.setColor(color);
            nm.notify(id, b.build());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }
    @Override protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }
    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        if (code == 1001) {
            boolean ok = r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED;
            if (ok) PrayerScheduler.scheduleAll(this);
            if (webView != null) {
                webView.post(() -> webView.evaluateJavascript(
                    "if(window.onNotifPermission)window.onNotifPermission("+ok+");", null));
            }
        }
    }
}

package com.vr.record;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class Licenses extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);
        WebView w= (WebView)findViewById(R.id.webViewLicenses);
        w.loadUrl("file:///android_asset/licenses.html");
    }
}

package com.armedarms.idealmedia.tools;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Picture;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.armedarms.idealmedia.R;

public class TwitterShare extends Dialog {
    ProgressDialog progress;
    String shareURL;

    View close;

    public TwitterShare(Context context, ProgressDialog progress, String shareURL) {
        super(context);
        this.progress = progress;
        this.shareURL = shareURL;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_twitter_share);

        close = findViewById(R.id.close_button);

        WebView mWebView = (WebView) findViewById(R.id.webWiew);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.loadUrl(shareURL);
        mWebView.setWebViewClient(new ThisWebViewClient());

        mWebView.setPictureListener(new WebView.PictureListener() {
            @Override
            public void onNewPicture(WebView view, Picture picture) {
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                TwitterShare.this.cancel();
            }
        });
    }

    class ThisWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);

            return true;
        }
    }
}
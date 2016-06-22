package net.olejon.mdapp;

/*

Copyright 2016 Ole Jon Bjørkum

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

*/

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class MedicationNlhFragment extends Fragment
{
    public static WebView WEBVIEW;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState)
    {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_medication_nlh, container, false);

        // Activity
        final Activity activity = getActivity();

        // Context
        final Context context = activity.getApplicationContext();

        // Tools
        final MyTools mTools = new MyTools(context);

        // Arguments
        Bundle bundle = getArguments();

        final String pageUri = bundle.getString("uri");

        // Progress bar
        final ProgressBar progressBar = (ProgressBar) viewGroup.findViewById(R.id.medication_nlh_progressbar);

        // Toolbar
        final LinearLayout toolbarSearchLayout = (LinearLayout) activity.findViewById(R.id.medication_toolbar_search_layout);
        final EditText toolbarSearchEditText = (EditText) activity.findViewById(R.id.medication_toolbar_search);

        // Web view
        WEBVIEW = (WebView) viewGroup.findViewById(R.id.medication_nlh_content);

        WebSettings webSettings = WEBVIEW.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCachePath(context.getCacheDir().getAbsolutePath());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        WEBVIEW.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                if(!mTools.isDeviceConnected())
                {
                    mTools.showToast(getString(R.string.device_not_connected), 0);
                    return true;
                }
                else if(url.matches(".*/[^#]+#[^/]+$"))
                {
                    WEBVIEW.loadUrl(url.replaceAll("#[^/]+$", ""));
                    return true;
                }
                else if(url.matches("^https?://.*?\\.pdf$"))
                {
                    mTools.downloadFile(view.getTitle(), url);
                    return true;
                }
                else if(url.startsWith("mailto:"))
                {
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(Intent.createChooser(intent, getString(R.string.project_feedback_text)));
                    return true;
                }
                else if(url.startsWith("tel:"))
                {
                    try
                    {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                        startActivity(intent);
                    }
                    catch(Exception e)
                    {
                        new MaterialDialog.Builder(context).title(getString(R.string.device_not_supported_dialog_title)).content(getString(R.string.device_not_supported_dialog_message)).positiveText(getString(R.string.device_not_supported_dialog_positive_button)).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
                    }

                    return true;
                }

                return false;
            }

            @Override
            public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler, SslError error)
            {
                handler.cancel();

                WEBVIEW.stopLoading();

                progressBar.setVisibility(View.INVISIBLE);

                new MaterialDialog.Builder(activity).title(getString(R.string.device_not_supported_dialog_title)).content(getString(R.string.device_not_supported_dialog_ssl_error_message)).positiveText(getString(R.string.device_not_supported_dialog_positive_button)).onPositive(new MaterialDialog.SingleButtonCallback()
                {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction)
                    {
                        WEBVIEW.goBack();
                    }
                }).cancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialogInterface)
                    {
                        WEBVIEW.goBack();
                    }
                }).contentColorRes(R.color.black).positiveColorRes(R.color.dark_blue).show();
            }
        });

        WEBVIEW.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public void onProgressChanged(WebView view, int newProgress)
            {
                if(newProgress > 32)
                {
                    progressBar.setVisibility(View.GONE);
                }
                else
                {
                    progressBar.setVisibility(View.VISIBLE);

                    toolbarSearchLayout.setVisibility(View.GONE);
                    toolbarSearchEditText.setText("");
                }
            }
        });

        if(savedInstanceState == null)
        {
            WEBVIEW.loadUrl(pageUri);
        }
        else
        {
            WEBVIEW.restoreState(savedInstanceState);
        }

        return viewGroup;
    }

    // Resume fragment
    @Override
    public void onResume()
    {
        super.onResume();

        WEBVIEW.resumeTimers();
    }

    // Pause fragment
    @Override
    public void onPause()
    {
        super.onPause();

        WEBVIEW.pauseTimers();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            //noinspection deprecation
            CookieSyncManager.getInstance().sync();
        }
    }

    // Save fragment
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        WEBVIEW.saveState(outState);
    }
}
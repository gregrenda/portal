/*
MIT License

Copyright (c) 2025 Greg Renda

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
*/

package org.renda.portal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.provider.Settings;
import android.net.Uri;

public class MainActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

	setContentView(R.layout.activity_main);
	setVisibleView(R.id.webView);

	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
	    !Settings.System.canWrite(this))
	{
	    Intent intent = new Intent(android.provider.Settings.
				       ACTION_MANAGE_WRITE_SETTINGS);
	    intent.setData(Uri.parse("package:" + this.getPackageName()));
	    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(intent);
	}
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed()
    {
	MyWebView w = findViewById(R.id.webView);

	if (w.canGoBack())
	{
	    w.goBack();
	}
	else
	{
	    super.onBackPressed();
	}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
	((AppSettings) findViewById(R.id.settings)).
	    onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume()
    {
	super.onResume();

	hideSystemUI();
    }

    @Override
    public void onPause()
    {
	super.onPause();

	setVisibleView(R.id.webView);
    }

    public void setVisibleView(int viewId)
    {
	for (int id : new int[] { R.id.webView, R.id.settings,
				  R.id.screenSaver })
	{
	    View v = findViewById(id);

	    if (v.getVisibility() == View.VISIBLE)
	    {
		v.setVisibility(View.GONE);
	    }
	}

	View v = findViewById(viewId);

        v.setVisibility(View.VISIBLE);

	hideSystemUI();

	// hide the soft keyboard
	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
	    hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @SuppressWarnings("deprecation")
    public void hideSystemUI()
    {
	View decorView = getWindow().getDecorView();

	decorView.
	    setSystemUiVisibility(decorView.getSystemUiVisibility() |
				  View.SYSTEM_UI_FLAG_LOW_PROFILE |
				  View.SYSTEM_UI_FLAG_FULLSCREEN |
				  View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
				  View.SYSTEM_UI_FLAG_IMMERSIVE |
				  View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}

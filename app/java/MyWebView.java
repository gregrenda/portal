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
import android.view.View;
import android.view.MotionEvent;
import android.content.Context;
import android.util.AttributeSet;
import android.os.CountDownTimer;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import java.util.Map;

public class MyWebView extends WebView
{
    private CountDownTimer screenSaverTimer;
    private AppSettings appSettings;
    private String url;

    public MyWebView(Context context, AttributeSet attrs)
    {
	super(context, attrs);

	WebSettings webSettings = getSettings();

	webSettings.setJavaScriptEnabled(true);
	webSettings.setDomStorageEnabled(true);

	setWebViewClient(new WebViewClient());
	clearCache(true);
    }

    @Override
    public void onAttachedToWindow()
    {
	super.onAttachedToWindow();

	MainActivity a = (MainActivity) getContext();
	appSettings = (AppSettings) a.findViewById(R.id.settings);

	appSettings.registerCallback(new SettingsCallback()
	    {
		public void onSettingsChanged()
		{
		    screenSaverTimer = new
			CountDownTimer(appSettings.
				       getInt(AppSettings.SCREEN_SAVER_TIMEOUT)
				       * 1000, 2000)
			{
			    @Override
			    public void onTick(long millisUntilFinished)
			    {
				a.hideSystemUI();
			    }

			    @Override
			    public void onFinish()
			    {
				a.setVisibleView(R.id.screenSaver);
			    }
			};

		    if (getVisibility() == View.VISIBLE)
		    {
			screenSaverTimer.start();
		    }

		    String newUrl = appSettings.getString(AppSettings.URL);

		    if (!newUrl.equals(url))
		    {
			url = newUrl;
			loadUrl(url);
		    }
		}
	    });

	setOnTouchListener(new View.OnTouchListener()
	{
	    @Override
	    public boolean onTouch(View v, MotionEvent event)
	    {
		int w = v.getWidth(), h = v.getHeight();
		float pct = 10;

		if (event.getX() >= w - w * (pct / 100) &&
		    event.getY() >= h - h * (pct / 100))
		{
		    a.setVisibleView(R.id.settings);
		}
		else
		{
		    screenSaverTimer.start();
		}

		return false;
	    }
	});

	if (!appSettings.valid())
	{
	    a.setVisibleView(R.id.settings);
	}
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility)
    {
	if (screenSaverTimer != null)
	{
	    if (visibility == View.VISIBLE)
	    {
		screenSaverTimer.start();
	    }
	    else
	    {
		screenSaverTimer.cancel();
	    }
	}
    }
}

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
import android.widget.TextView;
import android.content.Context;
import android.content.ContentResolver;
import android.util.AttributeSet;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.graphics.Color;
import java.util.Date;
import java.util.Random;
import java.util.Objects;
import java.util.Map;

public class ScreenSaver extends TextView
{
    private int insideTemp = -1, outsideTemp = -1, originalBrightness;
    private CountDownTimer moveTimer, insideTempTimer, outsideTempTimer;
    private MQTT mqtt;
    private AppSettings appSettings;

    class Callback implements Handler.Callback
    {
	@Override
	public boolean handleMessage(Message msg)
	{
	    Bundle b = msg.getData();
	    int value = Math.round(Float.parseFloat(b.getString("value")));
	    boolean update = false;

	    if (Objects.equals(b.getString("topic"),
			       appSettings.
			       getString(AppSettings.OUTSIDE_TEMP_TOPIC)))
	    {
		if (outsideTemp != value)
		{
		    outsideTemp = value;
		    update = true;

		    if (outsideTempTimer != null)
		    {
			outsideTempTimer.start();
		    }
		}
	    }
	    else if (insideTemp != value)
	    {
		insideTemp = value;
		update = true;

		if (insideTempTimer != null)
		{
		    insideTempTimer.start();
		}
	    }

	    if (update && getVisibility() == View.VISIBLE)
	    {
		display();
	    }

	    return true;
	}
    }

    public ScreenSaver(Context context, AttributeSet attrs)
    {
	super(context, attrs);

	moveTimer = new CountDownTimer(30000, 300000)
	    {
		@Override
		public void onTick(long millisUntilFinished) {}

		@Override
		public void onFinish()
		{
		    display();
		}
	    };
    }

    @Override
    public void onAttachedToWindow()
    {
	super.onAttachedToWindow();

	((View) getParent()).setOnTouchListener(new View.OnTouchListener()
	    {
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
		    ((MainActivity) getContext()).setVisibleView(R.id.webView);
		    return true;
		}
	    });

	appSettings = (AppSettings) ((Activity) getContext()).
		       findViewById(R.id.settings);

	appSettings.registerCallback(new SettingsCallback()
	    {
		public void onSettingsChanged()
		{
		    if (mqtt != null)
		    {
			mqtt.terminate();
		    }

		    mqtt = new MQTT(new Handler(Looper.getMainLooper(),
						new Callback()),
				    appSettings.getString(AppSettings.BROKER),
				    new String[] { appSettings.getString(AppSettings.OUTSIDE_TEMP_TOPIC),
						   appSettings.getString(AppSettings.INSIDE_TEMP_TOPIC)});
		}
	    });
    }

    @Override
    public void setVisibility(int visibility)
    {
	super.setVisibility(visibility);

	ContentResolver cr = getContext().getContentResolver();

	if (visibility == View.VISIBLE)
	{
	    try
	    {
		Settings.System.putInt(cr,
				       Settings.System.SCREEN_BRIGHTNESS_MODE,
				       Settings.System.
				       SCREEN_BRIGHTNESS_MODE_MANUAL);

		originalBrightness =
		    Settings.System.getInt(cr,
					   Settings.System.SCREEN_BRIGHTNESS);

		Settings.System.putInt(cr,
				       Settings.System.SCREEN_BRIGHTNESS,
				       (int) (255 * appSettings.getInt(AppSettings.SCREEN_SAVER_BRIGHTNESS) / 100));
	    }
	    catch (Exception e)
	    {
		e.printStackTrace();
	    }

	    int topicTimeout =
		appSettings.getInt(AppSettings.TOPIC_TIMEOUT) * 1000;

	    (insideTempTimer = new CountDownTimer(topicTimeout, 300000)
		{
		    @Override
		    public void onTick(long millisUntilFinished) {}

		    @Override
		    public void onFinish()
		    {
			insideTemp = -1;
		    }
		}).start();

	    (outsideTempTimer = new CountDownTimer(topicTimeout, 300000)
		{
		    @Override
		    public void onTick(long millisUntilFinished) {}

		    @Override
		    public void onFinish()
		    {
			outsideTemp = -1;
		    }
		}).start();

	    display();
	}
	else
	{
	    if (originalBrightness != 0)
	    {
		Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS,
				       originalBrightness);
	    }

	    moveTimer.cancel();
	}
    }

    @SuppressWarnings("deprecation")
    private void display()
    {
	Date date = new Date();
	String time = DateUtils.formatDateTime(getContext(), date.getTime(),
					       DateUtils.FORMAT_SHOW_TIME),
	    day = DateUtils.formatDateTime(getContext(), date.getTime(),
					   DateUtils.FORMAT_SHOW_WEEKDAY |
					   DateUtils.FORMAT_SHOW_DATE),
	    temps = new String("In: " + insideTemp +
			       "\u00b0 Out: " + outsideTemp + "\u00b0");
	SpannableString s = new SpannableString(time + "\n" + day + "\n\n" +
						temps);
	s.setSpan(new RelativeSizeSpan(.20f), time.length() + 1,
		  time.length() + day.length() + 2, 0);
	s.setSpan(new RelativeSizeSpan(.30f), time.length() + day.length() + 2,
		  time.length() + day.length() + 3 + temps.length(), 0);

	int color;
	try
	{
	    color = Color.parseColor(appSettings.
				     getString(AppSettings.SCREEN_SAVER_COLOR));
	}
	catch (Exception e)
	{
	    color = Color.parseColor("white");
	};

	setTextColor(color);
	setTextSize(appSettings.getInt(AppSettings.SCREEN_SAVER_FONT_SIZE));
	setText(s);

	moveTimer.start();
	int w = getWidth(), h = getHeight();

	if (w != 0)
	{
	    View parent = (View) getParent();
	    int maxX = parent.getWidth() - w, maxY = parent.getHeight() - h;

	    Random r = new Random();
	    setX(r.nextInt(maxX));
	    setY(r.nextInt(maxY));
	}
    }
}

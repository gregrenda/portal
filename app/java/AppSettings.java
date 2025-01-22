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

import android.net.Uri;
import android.app.Activity;
import android.provider.DocumentsContract;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.MotionEvent;
import android.os.CountDownTimer;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ScrollView;
import android.util.AttributeSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

interface SettingsCallback
{
    void onSettingsChanged();
};

public class AppSettings extends ScrollView implements View.OnClickListener
{
    public static final String URL = "url", BROKER = "broker",
	INSIDE_TEMP_TOPIC = "insideTemperatureTopic",
	OUTSIDE_TEMP_TOPIC = "outsideTemperatureTopic",
	TOPIC_TIMEOUT = "topicTimeout",
	SCREEN_SAVER_BRIGHTNESS = "screenSaverBrightness",
	SCREEN_SAVER_TIMEOUT = "screenSaverTimeout",
	SCREEN_SAVER_FONT_SIZE = "screenSaverFontSize",
	SCREEN_SAVER_COLOR = "screenSaverColor";

    private boolean fileDialog;
    private CountDownTimer cancelTimer;
    private Map<String, ?> settings;
    private List<SettingsCallback> settingsCallbacks = new ArrayList<>();
    private static final String
	VAL = "validate",
	DEF = "default",
	ID = "id",
	PREFS = "settings";
    private static final int EXPORT_REQUEST_CODE = 0, IMPORT_REQUEST_CODE = 1;
    private static final Map<String, Map<String, String>> fields =
	Map.of(URL,
	       Map.of(ID, "" + R.id.url,
		      DEF, "",
		      VAL, "notEmpty"),
	       BROKER,
	       Map.of(ID, "" + R.id.broker,
		      DEF, "",
		      VAL, "notEmpty"),
	       INSIDE_TEMP_TOPIC,
	       Map.of(ID, "" + R.id.insideTemperatureTopic,
		      DEF, "",
		      VAL, "notEmpty"),
	       OUTSIDE_TEMP_TOPIC,
	       Map.of(ID, "" + R.id.outsideTemperatureTopic,
		      DEF, "",
		      VAL, "notEmpty"),
	       TOPIC_TIMEOUT,
	       Map.of(ID, "" + R.id.topicTimeout,
		      DEF, "300",
		      VAL, "notZero"),
	       SCREEN_SAVER_TIMEOUT,
	       Map.of(ID, "" + R.id.screenSaverTimeout,
		      DEF, "60",
		      VAL, "notZero"),
	       SCREEN_SAVER_BRIGHTNESS,
	       Map.of(ID, "" + R.id.screenSaverBrightness,
		      DEF, "25",
		      VAL, "isPercent"),
	       SCREEN_SAVER_FONT_SIZE,
	       Map.of(ID, "" + R.id.screenSaverFontSize,
		      DEF, "130",
		      VAL, "notZero"),
	       SCREEN_SAVER_COLOR,
	       Map.of(ID, "" + R.id.screenSaverColor,
		      DEF, "white",
		      VAL, "notEmpty"));

    public AppSettings(Context context, AttributeSet attrs)
    {
	super(context, attrs);

	settings =
	    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getAll();

	cancelTimer = new CountDownTimer(60000, 2000)
	    {
		MainActivity a = (MainActivity) getContext();

		@Override
		public void onTick(long millisUntilFinished)
		{
		    a.hideSystemUI();
		}

		@Override
		public void onFinish()
		{
		    if (valid())
		    {
			a.setVisibleView(R.id.webView);
		    }
		}
	    };
    }

    @Override
    protected void onFinishInflate()
    {
	for (int id : new int[] { R.id.buttonSave, R.id.buttonCancel,
				  R.id.buttonImport, R.id.buttonExport})
	{
	    ((Button) findViewById(id)).setOnClickListener(this);
	}

	try
	{
	    MainActivity a = (MainActivity) getContext();
	    String ver = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;

	    ((TextView) findViewById(R.id.info)).
		setText("Copyright \u00a9 2025 Greg Renda\nVersion: " + ver);
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
	cancelTimer.start();
	return false;
    }

    @Override
    protected void onVisibilityChanged(View v, int visibility)
    {
	if (visibility == View.VISIBLE)
	{
	    if (fileDialog)
	    {
		fileDialog = false;
	    }
	    else
	    {
		setFields(settings);
	    }
	    cancelTimer.start();
	}
	else if (cancelTimer != null)
	{
	    cancelTimer.cancel();
	}
    }

    public void onClick(View v)
    {
	MainActivity a = (MainActivity) getContext();

	// don't allow cancel if current settings are not valid
	if (v.getId() == R.id.buttonCancel)
	{
	    if (!valid())
	    {
		Toast.makeText(a, "Settings are not valid",
			       Toast.LENGTH_LONG).show();
		return;
	    }
	}
	else if (v.getId() == R.id.buttonSave)
	{
	    Map<String,String> s = getFields();

	    if (!valid(s))
	    {
		Toast.makeText(a, "Settings are not valid",
			       Toast.LENGTH_LONG).show();
		return;
	    }

	    settings = s;

	    SharedPreferences.Editor edit =
		a.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();

	    for (String field : settings.keySet())
	    {
		edit.putString(field, (String) settings.get(field));
	    }

	    edit.commit();

	    for (SettingsCallback cb : settingsCallbacks)
	    {
		cb.onSettingsChanged();
	    }
	}
	else if (v.getId() == R.id.buttonExport)
	{
	    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

	    String name = a.getPackageName();

	    intent.addCategory(Intent.CATEGORY_OPENABLE)
		.setType("*/*")
		.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
			  Environment.DIRECTORY_DOWNLOADS)
		.putExtra(Intent.EXTRA_TITLE,
			  name.substring(name.lastIndexOf('.') + 1) + ".cfg");

	    fileDialog = true;
	    a.startActivityForResult(intent, EXPORT_REQUEST_CODE);
	    return;
	}
	else if (v.getId() == R.id.buttonImport)
	{
	    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

	    intent.addCategory(Intent.CATEGORY_OPENABLE)
		.setType("*/*")
		.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
			  Environment.DIRECTORY_DOWNLOADS);

	    fileDialog = true;
	    a.startActivityForResult(intent, IMPORT_REQUEST_CODE);
	    return;
	}

	a.setVisibleView(R.id.screenSaver);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
	if (resultCode == Activity.RESULT_OK)
	{
	    switch (requestCode)
	    {
		case EXPORT_REQUEST_CODE:
		    exportSettings(data.getData());
		    break;
		case IMPORT_REQUEST_CODE:
		    importSettings(data.getData());
		    return;
	    }
	}
    }

    private Map<String,String> getFields()
    {
	Map<String,String> s = new HashMap<String, String>();

	for (String field : fields.keySet())
	{
	    s.put(field, ((EditText)
			  findViewById(Integer.parseInt(fields.get(field).
							get(ID)))).
		  getText().toString().trim());
	}

	return s;
    }

    private void setFields(Map s)
    {
	for (String field : fields.keySet())
	{
	    ((EditText) findViewById(Integer.parseInt(fields.get(field).
						      get(ID)))).
		setText(s.containsKey(field) ? (String) s.get(field) :
			fields.get(field).get(DEF), TextView.BufferType.NORMAL);
	}
    }

    private boolean importSettings(Uri uri)
    {
	try
	{
	    ObjectInputStream in = new
		ObjectInputStream(getContext().getContentResolver().
				   openInputStream(uri));

	    setFields((Map) in.readObject());
	}
	catch (Exception e)
	{
	    return false;
	}

	return true;
    }

    private boolean exportSettings(Uri uri)
    {
	try
	{
	    ObjectOutputStream out = new
		ObjectOutputStream(getContext().getContentResolver().
				  openOutputStream(uri));
	    out.writeObject(getFields());
	    out.close();
	}
	catch (Exception e)
	{
	    return false;
	}

	return true;
    }

    private boolean valid(Map s)
    {
	for (String field : fields.keySet())
	{
	    try
	    {
		java.lang.reflect.Method method =
		    this.getClass().
		    getDeclaredMethod(fields.get(field).get(VAL),
				      Map.class, String.class);

		if (!(boolean) method.invoke(this, s, field))
		{
		    return false;
		}
	    }
	    catch (Exception e)
	    {
		e.printStackTrace();
	    }
	}

	return true;
    }

    private boolean notZero(Map s, String topic)
    {
	return s.containsKey(topic) && getInt(topic) > 0;
    }

    private boolean notEmpty(Map s, String topic)
    {
	return s.containsKey(topic) && !((String) s.get(topic)).isEmpty();
    }

    private boolean isPercent(Map s, String topic)
    {
	int v = getInt(topic);

	return v > 0 && v <= 100;
    }

    public boolean valid()
    {
	return valid(settings);
    }

    public String getSetting(String setting)
    {
	return (String) settings.get(setting);
    }

    public void registerCallback(SettingsCallback cb)
    {
	settingsCallbacks.add(cb);

	if (!settings.isEmpty())
	{
	    cb.onSettingsChanged();
	}
    }

    public int getInt(String key)
    {
	return Integer.parseInt((String) settings.get(key));
    }

    public String getString(String key)
    {
	return (String) settings.get(key);
    }
}

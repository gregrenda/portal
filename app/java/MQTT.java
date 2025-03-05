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

import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;

public class MQTT
{
    private static final int CONNECT = 1, CONNECT_ACK = 2, PUBLISH = 3,
	SUBSCRIBE = 8, SUBSCRIBE_ACK = 9, PINGREQ = 12, PINGRESP = 13,
	KEEP_ALIVE_SECONDS = 60, TIMEOUT_SECONDS = 60;
    private static final byte[] CONNECT_MSG =
        { CONNECT << 4, 0x0c, 0, 4, 'M', 'Q', 'T', 'T', 4, 2, 0,
	  KEEP_ALIVE_SECONDS, 0, 0 };
    private static final byte[] PINGREQ_MSG = {(byte) (PINGREQ << 4), 0 };
    private Thread thread;

    private class Msg
    {
	byte[] data;
	int payload;
    };

    public MQTT(Handler handler, String broker, String[] topics)
    {
	(thread = new Thread(new Runnable()
	    {
		@Override
		public void run()
		{
		    mainThread(handler, broker, topics);
		}
	    })).start();
    }

    public void terminate()
    {
	thread.interrupt();
    }

    private void mainThread(Handler handler, String broker, String[] topics)
    {
	while (!thread.isInterrupted())
	{
	    try
	    {
		String[] hostAndPort = broker.split(":");
		Socket socket = new Socket(hostAndPort[0],
					   Integer.parseInt(hostAndPort[1]));
		OutputStream out = socket.getOutputStream();
		int timeoutSecs = 20;
		long lastSend, lastRecv = 0;
		boolean connected = false;

		out.write(CONNECT_MSG);
		lastSend = System.currentTimeMillis();

		while (!thread.isInterrupted())
		{
		    Msg msg = readMessage(socket, timeoutSecs);
		    long now = System.currentTimeMillis();

		    if (msg != null)
		    {
			lastRecv = now;

			switch ((msg.data[0] >> 4) & 0xf)
			{
			    case CONNECT_ACK:
			    {
				connected = true;
				int len = 4;

				for (String s : topics)
				{
				    len += s.length() + 3;
				}

				byte[] sub = new byte[len];
				int n = 0;
				sub[n++] = (byte) (SUBSCRIBE << 4) | 2;
				sub[n++] = (byte) (len - 2);
				sub[n++] = 0;
				sub[n++] = 1;

				for (String s : topics)
				{
				    int l = s.length();
				    sub[n++] = 0;
				    sub[n++] = (byte) l;

				    System.arraycopy(s.getBytes(), 0, sub, n, l);
				    n += l;
				    sub[n++] = 0; // qos
				}

				out.write(sub);
				lastSend = lastRecv = now;
				break;
			    }
//			    case SUBSCRIBE_ACK:
//				break;
			    case PUBLISH:
			    {
				int len = (msg.data[msg.payload] << 4) |
				    msg.data[msg.payload + 1];
				Bundle b = new Bundle();
				b.putString("topic",
					    new String(msg.data,
						       msg.payload + 2, len));
				b.putString("value",
					    new String(msg.data,
						       msg.payload + 2 + len,
						       msg.data.length -
						       msg.payload - (2 + len)));
				Message m = Message.obtain(handler);
				m.setData(b);
				handler.sendMessage(m);
				break;
			    }
//			    case PINGRESP:
//				break;
			}
		    }
		    else
		    {
			if (now - lastRecv >= TIMEOUT_SECONDS * 1000)
			{
			    break;
			}

			out.write(PINGREQ_MSG);
			lastSend = now;
		    }

		    timeoutSecs = (int) (KEEP_ALIVE_SECONDS / 2 * 1000 -
					 (now - lastSend)) / 1000;
		}

		socket.close();
	    }
	    catch (Exception e)
	    {
	    }

	    if (thread.isInterrupted())
	    {
		break;
	    }

	    try
	    {
		Thread.sleep(5000);
	    }
	    catch (InterruptedException e)
	    {
		e.printStackTrace();
		break;
	    }
	}
    }

    private Msg readMessage(Socket s, int timeoutSecs)
    {
	Msg msg = new Msg();

	try
	{
	    byte[] h = new byte[5];
	    InputStream in = s.getInputStream();
	    int got = 0, len = 0, m = 1;

	    if (timeoutSecs <= 0)
	    {
		timeoutSecs = 1;
	    }

	    s.setSoTimeout(timeoutSecs * 1000);

	    // get the type
	    if (in.read(h, got++, 1) != 1)
	    {
		return null;
	    }

	    s.setSoTimeout(5000);

	    // get the remaining length
	    do
	    {
		if (in.read(h, got, 1) != 1)
		{
		    return null;
		}

		len += (h[got] & 0x7f) * m;
		m *= 128;
	    } while ((h[got++] & 0x80) == 0x80 && got < h.length);

	    msg.data = new byte[got + len];
	    System.arraycopy(h, 0, msg.data, 0, got);
	    msg.payload = got;

	    while (got < msg.data.length)
	    {
		if (in.read(msg.data, got++, 1) != 1)
		{
		    return null;
		}
	    }
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	    return null;
	}

	return msg;
    }
}

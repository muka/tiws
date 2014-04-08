//
//   Copyright 2012 jordi domenech <jordi@iamyellow.net>
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package net.iamyellow.tiws;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.TiContext.OnLifecycleEvent;

import java.net.URI;
import java.net.URISyntaxException;

import android.R.bool;
import android.app.Activity;

import com.codebutler.android_websockets.WebSocketClient;

@Kroll.proxy(creatableInModule = TiwsModule.class)
public class WSProxy extends KrollProxy implements OnLifecycleEvent {
	private WebSocketClient client;
	private boolean connected = false;

	// Constructor
	public WSProxy() {
		super();
	}

	// Websocket stuff
	private void cleanup() {
		if (client == null || !connected) {
			return;
		}

		connected = false;
		try {
			client.disconnect();
		} 
		catch (Exception ex) {
		}
		client = null;
		
		if (TiwsModule.DBG) {
			Log.d(TiwsModule.LCAT, "* websocket destroyed");
		}
	}

	// Context Lifecycle events
	@Override
	public void onStart(Activity activity) {
	}

	@Override
	public void onStop(Activity activity) {
	}

	@Override
	public void onPause(Activity activity) {
	}

	@Override
	public void onResume(Activity activity) {
	}

	@Override
	public void onDestroy(Activity activity) {
		cleanup();
	}

	// Handle creation options
	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);
	}

	// Methods

	@Kroll.method
	public void open(String uri) {
		final KrollProxy self = this;

		try {
			if (TiwsModule.DBG) {
				Log.d(TiwsModule.LCAT, "* creating websocket");
			}
			
			client = new WebSocketClient(new URI(uri), new WebSocketClient.Handler() {
				@Override
	            public void onMessage(byte[] data) {
					if (client == null) {
						return;
					}					
				}
				
				@Override
				public void onMessage(String message) {
					if (client == null) {
						return;
					}

					KrollDict event = new KrollDict();
					event.put("data", message);
					self.fireEvent("message", event);
				}

				@Override
				public void onError(Exception error) {
					if (client == null) {
						return;
					}
					
					if (TiwsModule.DBG) {
						Log.d(TiwsModule.LCAT, "* websocket error", error);
					}

					KrollDict event = new KrollDict();
					event.put("advice", "reconnect");
					event.put("error", error.toString());
					self.fireEvent("error", event);

					cleanup();
				}

				@Override
				public void onDisconnect(int code, String reason) {
					if (client == null) {
						return;
					}

					if (TiwsModule.DBG) {
						Log.d(TiwsModule.LCAT, "* creating disconnected; reason = " + reason + "; code = " + String.valueOf(code));
					}
					KrollDict event = new KrollDict();
					event.put("code", code);
					event.put("reason", reason);
					self.fireEvent("close", event);

					cleanup();
				}

				@Override
				public void onConnect() {
					connected = true;
					
					KrollDict event = new KrollDict();
					self.fireEvent("open", event);
				}
			}, null);
			
			client.connect();
		} 
		catch (URISyntaxException ex) {
			if (TiwsModule.DBG) {
				Log.d(TiwsModule.LCAT, "* creating exception", ex);
			}
			cleanup();
		}
	}

	@Kroll.method
	public boolean isConnected() {
		return connected;
	}	

	@Kroll.method
	public void close() {
		cleanup();
	}

	@Kroll.method
	public void send(String message) {
		if (client != null && connected) {
			client.send(message);
		}
	}
}
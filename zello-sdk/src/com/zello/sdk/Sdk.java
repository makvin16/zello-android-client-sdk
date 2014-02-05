package com.zello.sdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

public class Sdk implements SafeHandlerEvents {

	private String _package = "";
	private Activity _activity;
	private SafeHandler<Sdk> _handler;
	private Events _events;
	private boolean _resumed;
	private String _activeTabAction = "com.zello.sdk." + Util.generateUuid();
	private Contact _selectedContact = new Contact();
	private MessageIn _messageIn = new MessageIn();
	private MessageOut _messageOut = new MessageOut();
	private AppState _appState = new AppState();
	private BroadcastReceiver _receiverPackage; // Broadcast receiver for package install broadcasts
	private BroadcastReceiver _receiverAppState; // Broadcast receiver for app state broadcasts
	private BroadcastReceiver _receiverMessageState; // Broadcast receiver for message state broadcasts
	private BroadcastReceiver _receiverContactSelected; // Broadcast receiver for selected contact broadcasts
	private BroadcastReceiver _receiverActiveTab; // Broadcast receiver for last selected contact list tab

	private static final int AWAKE_TIMER = 1;

	private static final String _pttActivityClass = "com.zello.sdk.Activity";

	public Sdk() {
	}

	public void getSelectedContact(Contact contact) {
		_selectedContact.copyTo(contact);
	}

	public void getMessageIn(MessageIn message) {
		_messageIn.copyTo(message);
	}

	public void getMessageOut(MessageOut message) {
		_messageOut.copyTo(message);
	}

	public void getAppState(AppState state) {
		_appState.copyTo(state);
	}

	@SuppressWarnings("deprecation")
	public void onCreate(String packageName, Activity activity, Events events) {
		_package = Util.toLowerCaseLexicographically(Util.emptyIfNull(packageName));
		_activity = activity;
		_events = events;
		_handler = new SafeHandler<Sdk>(this);
		_appState._available = isAppAvailable();
		if (activity != null) {
			// Register to receive package install broadcasts
			_receiverPackage = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateAppAvailable();
					if (intent != null) {
						String action = intent.getAction();
						if (action != null) {
							if (action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE) || action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)) {
								String[] pkgs = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
								if (pkgs != null) {
									for (int i = 0; i < pkgs.length; ++i) {
										if (pkgs[i].equalsIgnoreCase(_package)) {
											updateSelectedContact(null);
											break;
										}
									}
								}
							} else {
								Uri data = intent.getData();
								if (data != null) {
									String pkg = data.getSchemeSpecificPart();
									if (pkg != null && pkg.equalsIgnoreCase(_package)) {
										updateSelectedContact(null);
									}
								}
							}
						}
					}
				}
			};
			IntentFilter filterPackage = new IntentFilter();
			filterPackage.addAction(Intent.ACTION_PACKAGE_ADDED);
			//noinspection deprecation
			filterPackage.addAction(Intent.ACTION_PACKAGE_INSTALL);
			filterPackage.addAction(Intent.ACTION_PACKAGE_REMOVED);
			filterPackage.addAction(Intent.ACTION_PACKAGE_REPLACED);
			filterPackage.addAction(Intent.ACTION_PACKAGE_CHANGED);
			filterPackage.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
			filterPackage.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
			filterPackage.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
			filterPackage.addDataScheme("package");
			activity.registerReceiver(_receiverPackage, filterPackage);
			// Register to receive app state broadcasts
			_receiverAppState = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateAppState(intent);
				}
			};
			Intent intentStickyAppState = activity.registerReceiver(_receiverAppState, new IntentFilter(_package + "." + Constants.ACTION_APP_STATE));
			updateAppState(intentStickyAppState);
			// Register to receive message state broadcasts
			_receiverMessageState = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateMessageState(intent);
				}
			};
			Intent intentStickyMessageState = activity.registerReceiver(_receiverMessageState, new IntentFilter(_package + "." + Constants.ACTION_MESSAGE_STATE));
			updateMessageState(intentStickyMessageState);
			// Register to receive selected contact broadcasts
			_receiverContactSelected = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateSelectedContact(intent);
				}
			};
			Intent intentStickySelectedContact = activity.registerReceiver(_receiverContactSelected, new IntentFilter(_package + "." + Constants.ACTION_CONTACT_SELECTED));
			updateSelectedContact(intentStickySelectedContact);
			// Register to receive last selected contact list tab
			_receiverActiveTab = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateSelectedTab(intent);
				}
			};
			activity.registerReceiver(_receiverActiveTab, new IntentFilter(_activeTabAction));
		}
	}

	public void onDestroy() {
		_resumed = false;
		Activity activity = _activity;
		if (activity != null) {
			activity.unregisterReceiver(_receiverPackage);
			activity.unregisterReceiver(_receiverAppState);
			activity.unregisterReceiver(_receiverMessageState);
			activity.unregisterReceiver(_receiverContactSelected);
			activity.unregisterReceiver(_receiverActiveTab);
		}
		_receiverPackage = null;
		_receiverAppState = null;
		_receiverMessageState = null;
		_receiverContactSelected = null;
		_receiverActiveTab = null;
		stopAwakeTimer();
		_handler = null;
		_activity = null;
		_events = null;
		_package = "";
	}

	public void onResume() {
		if (!_resumed) {
			_resumed = true;
			sendStayAwake();
			startAwakeTimer();
		}
	}

	public void onPause() {
		_resumed = false;
		stopAwakeTimer();
	}

	public void selectContact(String title, Tab[] tabs, Tab activeTab, Theme theme) {
		Activity activity = _activity;
		if (activity != null) {
			String tabList = tabsToString(tabs);
			if (tabList != null) {
				try {
					Intent intent = new Intent();
					intent.setComponent(new ComponentName(_package, _pttActivityClass));
					intent.setAction(Intent.ACTION_PICK);
					intent.putExtra(Intent.EXTRA_TITLE, title); // Activity title; optional
					intent.putExtra(Constants.EXTRA_TABS, tabList); // Set of displayed tabs; required; any combination of RECENTS, USERS and CHANNELS
					intent.putExtra(Constants.EXTRA_TAB, tabToString(activeTab)); // Initially active tab; optional; can be RECENTS, USERS or CHANNELS
					intent.putExtra(Constants.EXTRA_CALLBACK, _activeTabAction); // Last selected tab callback action; optional
					if (theme == Theme.LIGHT) {
						intent.putExtra(Constants.EXTRA_THEME, Constants.VALUE_LIGHT);
					}
					activity.startActivityForResult(intent, 0);
				} catch (Exception e) {
					// ActivityNotFoundException
				}
			}
		}
	}

	public void beginMessage() {
		Activity activity = _activity;
		if (activity != null) {
			Intent intent = new Intent(_package + "." + Constants.ACTION_COMMAND);
			intent.putExtra(Constants.EXTRA_COMMAND, Constants.VALUE_BEGIN_MESSAGE);
			activity.sendBroadcast(intent);
		}
	}

	public void endMessage() {
		Activity activity = _activity;
		if (activity != null) {
			Intent intent = new Intent(_package + "." + Constants.ACTION_COMMAND);
			intent.putExtra(Constants.EXTRA_COMMAND, Constants.VALUE_END_MESSAGE);
			activity.sendBroadcast(intent);
		}
	}

	public void setStatus(Status status) {
		Activity activity = _activity;
		if (activity != null) {
			Intent intent = new Intent(_package + "." + Constants.ACTION_COMMAND);
			intent.putExtra(Constants.EXTRA_COMMAND, Constants.VALUE_SET_STATUS);
			intent.putExtra(Constants.EXTRA_STATE_BUSY, status == Status.BUSY);
			intent.putExtra(Constants.EXTRA_STATE_SOLO, status == Status.SOLO);
			activity.sendBroadcast(intent);
		}
	}

	public void setStatusMessage(String message) {
		Activity activity = _activity;
		if (activity != null) {
			Intent intent = new Intent(_package + "." + Constants.ACTION_COMMAND);
			intent.putExtra(Constants.EXTRA_COMMAND, Constants.VALUE_SET_STATUS);
			intent.putExtra(Constants.EXTRA_STATE_STATUS_MESSAGE, Util.emptyIfNull(message));
			activity.sendBroadcast(intent);
		}
	}

	public void openMainScreen() {
		Activity activity = _activity;
		if (activity != null) {
			try {
				Intent LaunchIntent = activity.getPackageManager().getLaunchIntentForPackage(_package);
				activity.startActivity(LaunchIntent);
			} catch (Exception e) {
				// PackageManager.NameNotFoundException, ActivityNotFoundException
			}
		}
	}

	private void sendStayAwake() {
		Activity activity = _activity;
		if (activity != null) {
			Intent intent = new Intent(_package + "." + Constants.ACTION_COMMAND);
			intent.putExtra(Constants.EXTRA_COMMAND, Constants.VALUE_STAY_AWAKE);
			activity.sendBroadcast(intent);
		}
	}

	@Override
	public void handleMessageFromSafeHandler(Message message) {
		if (message != null) {
			if (message.what == AWAKE_TIMER) {
				if (_resumed) {
					sendStayAwake();
					Handler h = _handler;
					if (h != null) {
						h.sendMessageDelayed(h.obtainMessage(AWAKE_TIMER), Constants.STAY_AWAKE_TIMEOUT);
					}
				}
			}
		}
	}

	private void startAwakeTimer() {
		if (_resumed) {
			Handler h = _handler;
			if (h != null) {
				h.sendMessageDelayed(h.obtainMessage(AWAKE_TIMER), Constants.STAY_AWAKE_TIMEOUT);
			}
		}
	}

	private void stopAwakeTimer() {
		Handler h = _handler;
		if (h != null) {
			h.removeMessages(AWAKE_TIMER);
		}
	}

	private void updateAppAvailable() {
		boolean available = isAppAvailable();
		if (available != _appState._available) {
			_appState._available = available;
			Events events = _events;
			if (events != null) {
				events.onAppStateChanged();
			}
		}
	}

	private void updateAppState(Intent intent) {
		_appState.reset();
		if (intent != null) {
			_appState._signedIn = intent.getBooleanExtra(Constants.EXTRA_STATE_SIGNED_IN, false);
			_appState._signingIn = intent.getBooleanExtra(Constants.EXTRA_STATE_SIGNING_IN, false);
			_appState._signingOut = intent.getBooleanExtra(Constants.EXTRA_STATE_SIGNING_OUT, false);
			_appState._reconnectTimer = intent.getIntExtra(Constants.EXTRA_STATE_RECONNECT_TIMER, -1);
			_appState._waitingForNetwork = intent.getBooleanExtra(Constants.EXTRA_STATE_WAITING_FOR_NETWORK, false);
			_appState._showContacts = intent.getBooleanExtra(Constants.EXTRA_STATE_SHOW_CONTACTS, false);
			_appState._busy = intent.getBooleanExtra(Constants.EXTRA_STATE_BUSY, false);
			_appState._solo = intent.getBooleanExtra(Constants.EXTRA_STATE_SOLO, false);
			_appState._statusMessage = intent.getStringExtra(Constants.EXTRA_STATE_STATUS_MESSAGE);
		}
		Events events = _events;
		if (events != null) {
			events.onAppStateChanged();
		}
	}

	private void updateMessageState(Intent intent) {
		boolean out = false;
		boolean in = false;
		if (intent != null) {
			out = intent.getBooleanExtra(Constants.EXTRA_MESSAGE_OUT, false);
			in = !out && intent.getBooleanExtra(Constants.EXTRA_MESSAGE_IN, false);
			if (out) {
				_messageOut._to._name = intent.getStringExtra(Constants.EXTRA_CONTACT_NAME);
				_messageOut._to._fullName = intent.getStringExtra(Constants.EXTRA_CONTACT_FULL_NAME);
				_messageOut._to._displayName = intent.getStringExtra(Constants.EXTRA_CONTACT_DISPLAY_NAME);
				_messageOut._to._type = intToContactType(intent.getIntExtra(Constants.EXTRA_CONTACT_TYPE, -1));
				_messageOut._to._status = intToContactStatus(intent.getIntExtra(Constants.EXTRA_CONTACT_STATUS, 0));
				_messageOut._to._statusMessage = intent.getStringExtra(Constants.EXTRA_CONTACT_STATUS_MESSAGE);
				_messageOut._to._usersCount = intent.getIntExtra(Constants.EXTRA_CHANNEL_USERS_COUNT, 0);
				_messageOut._to._usersTotal = intent.getIntExtra(Constants.EXTRA_CHANNEL_USERS_TOTAL, 0);
				_messageOut._active = true;
				_messageOut._connecting = intent.getBooleanExtra(Constants.EXTRA_MESSAGE_CONNECTING, false);
			}
			if (in) {
				_messageIn._from._name = intent.getStringExtra(Constants.EXTRA_CONTACT_NAME);
				_messageIn._from._fullName = intent.getStringExtra(Constants.EXTRA_CONTACT_FULL_NAME);
				_messageIn._from._displayName = intent.getStringExtra(Constants.EXTRA_CONTACT_DISPLAY_NAME);
				_messageIn._from._type = intToContactType(intent.getIntExtra(Constants.EXTRA_CONTACT_TYPE, -1));
				_messageIn._from._status = intToContactStatus(intent.getIntExtra(Constants.EXTRA_CONTACT_STATUS, 0));
				_messageIn._from._statusMessage = intent.getStringExtra(Constants.EXTRA_CONTACT_STATUS_MESSAGE);
				_messageIn._from._usersCount = intent.getIntExtra(Constants.EXTRA_CHANNEL_USERS_COUNT, 0);
				_messageIn._from._usersTotal = intent.getIntExtra(Constants.EXTRA_CHANNEL_USERS_TOTAL, 0);
				_messageIn._author._name = intent.getStringExtra(Constants.EXTRA_CHANNEL_AUTHOR_NAME);
				_messageIn._author._fullName = intent.getStringExtra(Constants.EXTRA_CHANNEL_AUTHOR_FULL_NAME);
				_messageIn._author._displayName = intent.getStringExtra(Constants.EXTRA_CHANNEL_AUTHOR_DISPLAY_NAME);
				_messageIn._author._status = intToContactStatus(intent.getIntExtra(Constants.EXTRA_CHANNEL_AUTHOR_STATUS, 0));
				_messageIn._author._statusMessage = intent.getStringExtra(Constants.EXTRA_CHANNEL_AUTHOR_STATUS_MESSAGE);
				_messageIn._active = true;
			}
		}
		if (!in) {
			_messageIn.reset();
		}
		if (!out) {
			_messageOut.reset();
		}
		Events events = _events;
		if (events != null) {
			events.onMessageStateChanged();
		}
	}

	private void updateSelectedContact(Intent intent) {
		String name = intent != null ? intent.getStringExtra(Constants.EXTRA_CONTACT_NAME) : null; // Contact name
		boolean selected = name != null && name.length() > 0;
		if (selected) {
			// Update info
			_selectedContact._name = name;
			_selectedContact._fullName = intent.getStringExtra(Constants.EXTRA_CONTACT_FULL_NAME);
			_selectedContact._displayName = intent.getStringExtra(Constants.EXTRA_CONTACT_DISPLAY_NAME);
			_selectedContact._type = intToContactType(intent.getIntExtra(Constants.EXTRA_CONTACT_TYPE, -1));
			_selectedContact._status = intToContactStatus(intent.getIntExtra(Constants.EXTRA_CONTACT_STATUS, 0));
			_selectedContact._statusMessage = intent.getStringExtra(Constants.EXTRA_CONTACT_STATUS_MESSAGE);
			_selectedContact._usersCount = intent.getIntExtra(Constants.EXTRA_CHANNEL_USERS_COUNT, 0);
			_selectedContact._usersTotal = intent.getIntExtra(Constants.EXTRA_CHANNEL_USERS_TOTAL, 0);
		} else {
			_selectedContact.reset();
		}
		Events events = _events;
		if (events != null) {
			events.onSelectedContactChanged();
		}
	}

	private void updateSelectedTab(Intent intent) {
		if (intent != null) {
			Tab tab = stringToTab(intent.getStringExtra(Constants.EXTRA_TAB));
			Events events = _events;
			if (events != null) {
				events.onLastContactsTabChanged(tab);
			}
		}
	}

	private boolean isAppAvailable() {
		Activity activity = _activity;
		if (activity != null) {
			try {
				return null != activity.getPackageManager().getLaunchIntentForPackage(_package);
			} catch (Exception e) {
				// PackageManager.NameNotFoundException
			}
		}
		return false;
	}

	private static ContactType intToContactType(int type) {
		switch (type) {
		case 1:
			return ContactType.CHANNEL;
		case 3:
			return ContactType.GROUP;
		case 2:
			return ContactType.GATEWAY;
		default:
			return ContactType.USER;
		}
	}

	private static ContactStatus intToContactStatus(int status) {
		switch (status) {
		case 1:
			return ContactStatus.STANDBY;
		case 2:
			return ContactStatus.AVAILABLE;
		case 3:
			return ContactStatus.BUSY;
		case 6:
			return ContactStatus.CONNECTING;
		default:
			return ContactStatus.OFFLINE;
		}
	}

	private static String tabToString(Tab tab) {
		switch (tab) {
			case RECENTS:
				return Constants.VALUE_RECENTS;
			case USERS:
				return Constants.VALUE_USERS;
			case CHANNELS:
				return Constants.VALUE_CHANNELS;
		}
		return null;
	}

	private static String tabsToString(Tab[] tabs) {
		String s = null;
		if (tabs != null) {
			for (int i = 0; i < tabs.length; ++i) {
				String tab = tabToString(tabs[i]);
				if (tab != null) {
					if (s == null) {
						s = tab;
					} else {
						s += "," + tab;
					}
				}
			}
		}
		return s;
	}

	private static Tab stringToTab(String s) {
		if (s.equals(Constants.VALUE_USERS)) {
			return Tab.USERS;
		}
		if (s.equals(Constants.VALUE_CHANNELS)) {
			return Tab.CHANNELS;
		}
		return Tab.RECENTS;
	}

}

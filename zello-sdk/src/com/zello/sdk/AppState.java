package com.zello.sdk;

/**
 * <pre>
 * The AppState class is a representation of the current state of the Zello for Work app at any given moment.
 * This class is useful for getting the status of the Zello for Work app.
 * </pre>
 * <pre>
 * To use, retrieve the current AppState instance from the Zello class using the getAppState() method. For specific usage, please see the sample projects.
 * </pre>
 */
public class AppState {

	//region Package Private Variables

	boolean _available;
	boolean _error; // Set when the service fails to connect
	boolean _initializing;
	boolean _customBuild;
	boolean _configuring;
	boolean _locked;
	boolean _signedIn;
	boolean _signingIn;
	boolean _signingOut;
	boolean _cancelling;
	int _reconnectTimer = -1;
	boolean _waitingForNetwork;
	boolean _showContacts;
	boolean _busy;
	boolean _solo;
	boolean _autoRun;
	boolean _autoChannels = true;
	Error _lastError = Error.NONE;
	String _statusMessage;
	String _network;
	String _networkUrl;
	String _username;
	String _externalId;

	//endregion

	/**
	 * The reset() method resets the AppState instance back to the default values.
	 */
	public void reset() {
		//_available = false;
		_customBuild = false;
		_configuring = false;
		_locked = false;
		_signedIn = false;
		_signingIn = false;
		_signingOut = false;
		_cancelling = false;
		_reconnectTimer = -1;
		_waitingForNetwork = false;
		_showContacts = false;
		_busy = false;
		_solo = false;
		_autoRun = false;
		_autoChannels = true;
		_statusMessage = null;
		_network = null;
		_networkUrl = null;
		_username = null;
		_externalId = null;
	}

	@Override
	public AppState clone() {
		AppState state = new AppState();
		copyTo(state);
		return state;
	}

	//region Public State Methods

	/**
	 * The isAvailable() method determines if the Zello for Work app is available on the device.
	 * @return boolean indicating if the app is available to communicate with.
     */
	public boolean isAvailable() {
		return _available && !_error;
	}

	/**
	 * The isInitializing() method determines if the Zello for Work app is initializing.
	 * @return boolean indicating if the app is initializing.
	 */
	public boolean isInitializing() {
		return _initializing;
	}

	/**
	 * The isCustomBuild() method determines if the Zello for Work app is a custom build.
	 * @return boolean indicating if the app is a custom build.
	 */
	public boolean isCustomBuild() {
		return _customBuild;
	}

	/**
	 * The isConfiguring() method determines if the Zello for Work app is currently configuring.
	 * @return boolean indicating if the app is currently configuring.
	 */
	public boolean isConfiguring() {
		return _configuring;
	}

	/**
	 * <pre>
	 * The isLocked() method determines if the Zello for Work app is currently locked.
	 * </pre>
	 * <pre>
	 * If the Zello for Work app is locked, the UI will only display an information screen with the name of your app that can be clicked to open the main activity.
	 * Being locked does NOT interfere with the sending and receiving of messages through the Zello for Work app.
	 * </pre>
	 * @return boolean indicating if the app is currently locked.
	 */
	public boolean isLocked() {
		return _locked;
	}

	/**
	 * The isSignedIn() method determines if the user is currently authenticated.
	 * @return boolean indicating if the user is signed in.
	 */
	public boolean isSignedIn() {
		return _signedIn;
	}

	/**
	 * The isSigningIn() method determines if the user is in the process of authenticating.
	 * @return boolean indicating if the user is being authenticated.
     */
	public boolean isSigningIn() {
		return _signingIn;
	}

	/**
	 * The isSigningOut() method determines if the user is in the process of signing out.
	 * @return boolean indicating if the user is signing out.
     */
	public boolean isSigningOut() {
		return _signingOut;
	}

	/**
	 * The isCancellingSignin() method determines if the authentication request for the user is being cancelled.
	 * @return boolean indicating if the authentication request is being cancelled.
     */
	public boolean isCancellingSignin() {
		return _cancelling;
	}

	/**
	 * The isReconnecting() method determines if the Zello for Work app is trying to reconnect the user.
	 * @return boolean indicating if the app is trying to reconnect the user.
     */
	public boolean isReconnecting() {
		return _reconnectTimer >= 0;
	}

	/**
	 * The isWaitingForNetwork() method determines if the Zello for Work app is waiting for the network to respond.
	 * @return boolean indicating if the app is waiting for the network.
     */
	public boolean isWaitingForNetwork() {
		return _waitingForNetwork;
	}

	/**
	 * <pre>
	 * The isAutoRunEnabled() method determines if the auto run setting is enabled.
	 * </pre>
	 * <pre>
	 * The auto run enabled feature determines if the app should be launched on the start of the OS or not.
	 * </pre>
	 * @return boolean indicating whether or not auto run is enabled.
	 */
	public boolean isAutoRunEnabled() {
		return _autoRun;
	}

	/**
	 * <pre>
	 * The isChannelAutoConnectEnabled() method determines if the auto connect channel setting is enabled.
	 * </pre>
	 * <pre>
	 * The auto connect channel feature determines whether or not any new channel that the authenticated user is added to will be automatically connected to.
	 * </pre>
	 * @return boolean indicating whether or not auto connect channels is enabled.
	 */
	public boolean isChannelAutoConnectEnabled() {
		return _autoChannels;
	}

	//endregion

	//region Public Getters

	/**
	 * The getReconnectTimer() method returns the timer for reconnecting to the network.
	 * @return The network reconnect timer in seconds.
     */
	public int getReconnectTimer() {
		return _reconnectTimer;
	}

	/**
	 * The getShowContacts() method determines if the contacts for the user should be shown or not.
	 * @return boolean indicating if the contacts should be shown or not.
     */
	public boolean getShowContacts() {
		return _showContacts;
	}

	/**
	 * The getStatus() method returns the Status for the authenticated user.
	 * @return The current Status for the user.
     */
	public Status getStatus() {
		return _busy ? Status.BUSY : (_solo ? Status.SOLO : Status.AVAILABLE);
	}

	/**
	 * The getStatusMessage() method returns the custom status message for the authenticated user.
	 * @return Nullable; The status message for the user.
     */
	public String getStatusMessage() {
		return _statusMessage;
	}

	/**
	 * The getNetwork() method returns the network String for the Zello for Work app.
	 * @return Nullable; The network String.
	 */
	public String getNetwork() {
		return _network;
	}

	/**
	 * The getNetworkUrl() method returns the network in URL format for the Zello for Work app.
	 * @return Nullable; The network URL.
	 */
	public String getNetworkUrl() {
		return _networkUrl;
	}

	/**
	 * The getUsername() method returns the username of the authenticated user.
	 * @return Nullable; The username for the user.
     */
	public String getUsername() {
		return _username;
	}

	/**
	 * The getLastError() method returns the most recent authentication Error encountered by the Zello for Work app.
	 * @return Error type indicating the latest error.
     */
	public Error getLastError() {
		return _lastError;
	}

	/**
	 * <pre>
	 * The getExternalId() method returns the external id for messages recorded on the server.
	 * </pre>
	 * <pre>
	 * The external id is a tag for messages recorded on the server.
	 * This tag is only recorded if the server recording feature is enabled on the Zello for Work console.
	 * </pre>
	 * @return Nullable; The external id for the app.
     */
	public String getExternalId() {
		return _externalId;
	}

	//endregion

	//region Package Private Methods

	void copyTo(AppState state) {
		if (state != null) {
			state._customBuild = _customBuild;
			state._available = _available;
			state._error = _error;
			state._initializing = _initializing;
			state._configuring = _configuring;
			state._locked = _locked;
			state._signedIn = _signedIn;
			state._signingIn = _signingIn;
			state._signingOut = _signingOut;
			state._reconnectTimer = _reconnectTimer;
			state._waitingForNetwork = _waitingForNetwork;
			state._showContacts = _showContacts;
			state._busy = _busy;
			state._solo = _solo;
			state._autoRun = _autoRun;
			state._autoChannels = _autoChannels;
			state._statusMessage = _statusMessage;
			state._network = _network;
			state._networkUrl = _networkUrl;
			state._username = _username;
			state._lastError = _lastError;
			state._externalId = _externalId;
		}
	}

	//endregion

}

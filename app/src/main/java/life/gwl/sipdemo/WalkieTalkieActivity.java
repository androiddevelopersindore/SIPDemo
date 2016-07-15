/*
*
*@auther rajendra
* */
package life.gwl.sipdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.ParseException;

/**
 * Handles all calling, receiving calls, and UI interaction in the WalkieTalkie app.
 */
public class WalkieTalkieActivity extends AppCompatActivity implements View.OnTouchListener {

    public String sipAddress = null;

    public SipManager mSipManager = null;
    public SipProfile mSipProfile = null;
    public SipAudioCall mSipAudioCall = null;
    public IncomingCallReceiver mIncomingCallReceiver;

    private static final int CALL_ADDRESS = 1;
    private static final int SET_AUTH_INFO = 2;
    private static final int UPDATE_SETTINGS_DIALOG = 3;
    private static final int HANG_UP = 4;
    private String Tag = "WalkieActivity";
/*
*
* */

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.walkietalkie);
        init();
        initIncomingCallReceiver();
        keepScreenOn();
        initializeManager();
    }

    /*
    *
    * */
    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    /*
    *
    *
    * */
    private void initIncomingCallReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        mIncomingCallReceiver = new IncomingCallReceiver();
        this.registerReceiver(mIncomingCallReceiver, filter);

    }

    /*
    *initialize the UI
    *
    * */
    private void init() {
        ToggleButton pushToTalkButton = (ToggleButton) findViewById(R.id.pushToTalk);
        pushToTalkButton.setOnTouchListener(this);

    }

    /*
    *
    *When we get back from the preference setting Activity, assume
     *  settings have changed, and re-login with new auth info
    * */
    @Override
    public void onStart() {
        super.onStart();
        initializeManager();
    }

    /*
    *
    * disconnect the SipAudiocall, if call is going
    * un register incoming call receiver
    * */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSipAudioCall != null) {
            mSipAudioCall.close();
        }

        closeLocalProfile();

        if (mIncomingCallReceiver != null) {
            this.unregisterReceiver(mIncomingCallReceiver);
        }
    }

    /*
    * it initial the sip manager
    *
    * */
    public void initializeManager() {
        if (mSipManager == null) {
            mSipManager = SipManager.newInstance(this);
        }

        initializeLocalProfile();
    }

    /*
    * this register the SipManager with credentail present in preference and set the registration listener
    * thorough SipManager
    * */
    public void initializeLocalProfile() {
        if (mSipManager == null) {
            return;
        }

        if (mSipProfile != null) {
            closeLocalProfile();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = prefs.getString("namePref", "");
        String domain = prefs.getString("domainPref", "");
        String password = prefs.getString("passPref", "");

        if (username.length() == 0 || domain.length() == 0 || password.length() == 0) {
            showDialog(UPDATE_SETTINGS_DIALOG);
            return;
        }

        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            mSipProfile = builder.build();

            Intent i = new Intent();
            i.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            mSipManager.open(mSipProfile, pi, null);


            // This listener must be added AFTER mSipManager.open is called,
            // Otherwise the methods aren't guaranteed to fire.

            mSipManager.setRegistrationListener(mSipProfile.getUriString(), new SipRegistrationListener() {
                public void onRegistering(String localProfileUri) {
                    Log.i(Tag, "Registering with SIP Server...");
                    updateStatus("Registering with SIP Server...");
                }

                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    updateStatus("Ready");
                    Log.i(Tag, "Ready");
                }

                public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                 String errorMessage) {
                    updateStatus("Registration failed.  Please check settings.");
                    Log.w(Tag, "Registration failed.  Please check settings.");
                    Log.e(Tag, "Error Code :" + errorCode);
                    Log.e(Tag, "Error Message :" + errorMessage);
                }

            });
        } catch (ParseException pe) {
            Log.w(Tag, " User information is not set");
            Log.e(Tag, " Name :" + pe.getMessage());
            updateStatus("Connection Error.");
        } catch (SipException se) {
            Log.w(Tag, " unable to register sip manager");
            Log.e(Tag, " Name :" + se.getMessage());

            updateStatus("Connection error.");
        }
    }

    /**
     * Closes out your local profile, freeing associated objects into memory
     * and unregistering your device from the server.
     */

    public void closeLocalProfile() {
        if (mSipManager == null) {
            return;
        }
        try {
            if (mSipProfile != null) {
                mSipManager.close(mSipProfile.getUriString());
            }
        } catch (Exception ee) {
            Log.d(Tag, "Failed to close local profile.", ee);
        }
    }

    /**
     * Make an outgoing mSipAudioCall.
     */
    public void makeOutgoingcall() {

        updateStatus(sipAddress);

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing mSipAudioCall, don't
                // forget to set up a listener to set things up once the mSipAudioCall is established.
                @Override
                public void onCallEstablished(SipAudioCall call) {
                    call.startAudio();
                    call.setSpeakerMode(true);
                    call.toggleMute();
                    updateStatus(call);
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    updateStatus("Ready.");
                }
            };

            mSipAudioCall = mSipManager.makeAudioCall(mSipProfile.getUriString(), sipAddress, listener, 30);

        } catch (Exception e) {
            Log.i(Tag, "Error when trying to close mSipManager.", e);
            if (mSipProfile != null) {
                try {
                    mSipManager.close(mSipProfile.getUriString());
                } catch (Exception ee) {
                    Log.i(Tag,
                            "Error when trying to close mSipManager.", ee);
                    ee.printStackTrace();
                }
            }
            if (mSipAudioCall != null) {
                mSipAudioCall.close();
            }
        }
    }

    /**
     * Updates the status box at the top of the UI with a messege of your choice.
     *
     * @param status The String to display in the status box.
     */
    public void updateStatus(final String status) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = (TextView) findViewById(R.id.sipLabel);
                labelView.setText(status);
            }
        });
    }

    /**
     * Updates the status box with the SIP address of the current mSipAudioCall.
     *
     * @param call The current, active mSipAudioCall.
     */
    public void updateStatus(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if (useName == null) {
            useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }

    /**
     * Updates whether or not the user's voice is muted, depending on whether the button is pressed.
     *
     * @param v     The View where the touch event is being fired.
     * @param event The motion to act on.
     * @return boolean Returns false to indicate that the parent view should handle the touch event
     * as it normally would.
     */
    public boolean onTouch(View v, MotionEvent event) {

        if (mSipAudioCall == null) {
            return false;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN && mSipAudioCall != null && mSipAudioCall.isMuted()) {
            mSipAudioCall.toggleMute();
        } else if (event.getAction() == MotionEvent.ACTION_UP && !mSipAudioCall.isMuted()) {
            mSipAudioCall.toggleMute();
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALL_ADDRESS, 0, "Call someone");
        menu.add(0, SET_AUTH_INFO, 0, "Edit your SIP Info.");
        menu.add(0, HANG_UP, 0, "End Current Call.");

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case SET_AUTH_INFO:
                updatePreferences();
                break;
            case HANG_UP:
                if (mSipAudioCall != null) {
                    try {
                        mSipAudioCall.endCall();
                    } catch (SipException se) {
                        Log.d("WalkieActivity",
                                "Error ending mSipAudioCall.", se);
                    }
                    mSipAudioCall.close();
                }
                break;
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CALL_ADDRESS:

                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.call_address_dialog, null);
                return new AlertDialog.Builder(this)
                        .setTitle("Call Someone.")
                        .setView(textBoxView)
                        .setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText textField = (EditText)
                                                (textBoxView.findViewById(R.id.calladdress_edit));
                                        sipAddress = textField.getText().toString();
                                        makeOutgoingcall();

                                    }
                                })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                                })
                        .create();

            case UPDATE_SETTINGS_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage("Please update your SIP Account Settings.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                updatePreferences();
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                                })
                        .create();
        }
        return null;
    }
    /*
    * update the preference through preference activity
    * */

    public void updatePreferences() {
        Intent settingsActivity = new Intent(getBaseContext(),
                SipSettings.class);
        startActivity(settingsActivity);
    }
}

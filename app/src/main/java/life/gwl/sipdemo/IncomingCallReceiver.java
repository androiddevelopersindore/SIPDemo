
package life.gwl.sipdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;
import android.util.Log;

/*
*
*
* */
public class IncomingCallReceiver extends BroadcastReceiver {
    private String Tag="IncomingCallReceiver";
    private Uri mUriRingtone;

    /*
    *
    *
    * */
    @Override
    public void onReceive(final Context context, Intent intent) {
        SipAudioCall incomingCall = null;
        try {

            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try {
                        playRingTone(context);
                        call.answerCall(30);
                    } catch (Exception e) {
                        Log.w(Tag, "Exception while ringing the call");
                        Log.e(Tag,"name :"+e);
                    }
                }

                @Override
                public void onCalling(SipAudioCall call) {
                    super.onCalling(call);
                }

                @Override
                public void onCallBusy(SipAudioCall call) {
                    super.onCallBusy(call);
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    super.onCallEnded(call);
                }

                @Override
                public void onCallEstablished(SipAudioCall call) {
                    super.onCallEstablished(call);
                }
            };

            WalkieTalkieActivity mWalkieTalkieActivity = (WalkieTalkieActivity) context;

            incomingCall = mWalkieTalkieActivity.mSipManager.takeAudioCall(intent, listener);
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);
            if(incomingCall.isMuted()) {
                incomingCall.toggleMute();
            }

            mWalkieTalkieActivity.mSipAudioCall = incomingCall;

            mWalkieTalkieActivity.updateStatus(incomingCall);

        } catch (Exception e) {
            if (incomingCall != null) {
                incomingCall.close();
            }
            Log.w(Tag,"unable to handle call");
            Log.e(Tag, "Name :"+e.getMessage());
        }
    }

    private void playRingTone(Context context) {
        if (mUriRingtone == null) {
            mUriRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        Ringtone ringtone = RingtoneManager.getRingtone(context, mUriRingtone);
        ringtone.play();
    }


}

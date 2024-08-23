package com.kalmar.blockcalls;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashSet;

public class BlockScreeningService extends CallScreeningService {
    SharedPreferences preferences;

    @Override
    public IBinder onBind(Intent intent) {
        preferences = this.getApplicationContext().getSharedPreferences(
                MainActivity.APP_PREF,
                Context.MODE_PRIVATE
        );
        return super.onBind(intent);
    }

    @Override
    public void onScreenCall(@NonNull Call.Details details) {
        boolean isIncoming = details.getCallDirection() == Call.Details.DIRECTION_INCOMING;
        boolean isEnabled = this.preferences.getInt(MainActivity.APP_PREF_ENABLED, 0) == 1;

        Log.i(TAG, "Calls block accepted call, aka blyad zvonyat");

        CallResponse.Builder response = new CallResponse.Builder();
        if(!isEnabled) {
            respondToCall(details, response.build());
            return;
        }

        if (isIncoming) {
            String number = details.getHandle()
                    .toString()
                    .replaceAll("\\D", "");
            HashSet<String> numbers = this.getNumbers();

            SharedPreferences.Editor editor = this.preferences.edit();
            editor.putInt(MainActivity.APP_PREF_LOADED_NUMBERS, numbers.size());

            if(!numbers.contains(number)) {
                editor.putInt(
                        MainActivity.APP_PREF_BLOCKED_CALLS,
                        this.preferences.getInt(
                                MainActivity.APP_PREF_BLOCKED_CALLS,
                                0
                        ) + 1
                );

                response.setDisallowCall(true);
                response.setRejectCall(true);
                response.setSkipNotification(true);
            }

            editor.apply();
        }

        respondToCall(details, response.build());
    }

    @SuppressLint("Range")
    private HashSet<String> getNumbers() {
        HashSet<String> numbers = new HashSet<String>();

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null
                    );
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        );
                        numbers.add(
                                phoneNo.replaceAll("\\D", "")
                        );
                    }
                    pCur.close();
                }
            }
        }
        if(cur!=null){
            cur.close();
        }

        return numbers;
    }
}
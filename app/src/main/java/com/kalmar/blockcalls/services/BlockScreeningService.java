package com.kalmar.blockcalls.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import androidx.annotation.NonNull;

import com.kalmar.blockcalls.utils.CallsHelper;
import com.kalmar.blockcalls.MainActivity;
import com.kalmar.blockcalls.utils.PauseParams;

import java.util.HashSet;

public class BlockScreeningService extends CallScreeningService {
    private SharedPreferences appPreferences;
    private CallsHelper callsHelper;

    @Override
    public IBinder onBind(Intent intent) {
        appPreferences = getSharedPreferences(
                MainActivity.APP_PREF,
                Context.MODE_MULTI_PROCESS
        );

        callsHelper = new CallsHelper();

        return super.onBind(intent);
    }

    @Override
    public void onScreenCall(@NonNull Call.Details details) {
        CallResponse.Builder response = new CallResponse.Builder();

        if (details.getCallDirection() == Call.Details.DIRECTION_INCOMING) {
            if(!(this.appPreferences.getInt(MainActivity.APP_PREF_STATUS, 0) == 1)) {
                respondToCall(details, response.build());
                return;
            }

            PauseParams pauseParams = PauseParams.getFromPreferences(getApplicationContext());

            if(pauseParams.isPaused()) {
                respondToCall(details, response.build());
                return;
            }

            SharedPreferences.Editor editor = this.appPreferences.edit();

            String number = callsHelper.processNumber(details.getHandle().toString());
            HashSet<String> numbers = callsHelper.getNumbers(this.getApplicationContext());

            if(!numbers.contains(number)) {
                editor.putInt(
                        MainActivity.APP_PREF_BLOCKED_CALLS,
                        this.appPreferences.getInt(
                                MainActivity.APP_PREF_BLOCKED_CALLS,
                                0
                        ) + 1
                );

                response.setDisallowCall(true);
                response.setRejectCall(true);
                response.setSkipNotification(true);
            }

            editor.putInt(
                    MainActivity.APP_PREF_LOADED_NUMBERS,
                    numbers.size()
            );

            editor.apply();
        }

        respondToCall(details, response.build());
    }
}
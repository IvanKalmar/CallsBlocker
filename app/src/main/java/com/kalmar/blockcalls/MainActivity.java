package com.kalmar.blockcalls;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.kalmar.blockcalls.databinding.ActivityMainBinding;
import com.kalmar.blockcalls.services.WidgetCounterService;
import com.kalmar.blockcalls.utils.CallsHelper;
import com.kalmar.blockcalls.utils.PauseParams;

public class MainActivity extends AppCompatActivity {
    private TextView serviceEnabler, clearCallsLog, resetTimer, blockerStatusText,
            bindScreeningServicePermissionValue, readContactsPermissionValue,
            callsAccessPermissionValue, loadedContactsValue, blockedCallsValue;

    private static final int ROLE_REQUEST_ID = 1;
    private boolean ROLE_GRANTED = false;
    private boolean READ_CONTACTS_GRANTED = false;
    private boolean CALLS_ACCESS_GRANTED = false;

    public static final String APP_PREF =
            "com.kalmar.BlockCalls.appPreferences";
    public static final String APP_PREF_STATUS =
            "com.kalmar.BlockCalls.appPreferences.status";
    public static final String APP_PREF_LOADED_NUMBERS =
            "com.kalmar.BlockCalls.appPreferences.loadedNumbers";
    public static final String APP_PREF_BLOCKED_CALLS =
            "com.kalmar.BlockCalls.appPreferences.blockedCalls";

    private SharedPreferences preferences;

    private CallsHelper callsHelper = new CallsHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e){}

        RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
        startActivityForResult(intent, ROLE_REQUEST_ID);

        this.initPreferences();

        com.kalmar.blockcalls.databinding.ActivityMainBinding binding =
                ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        this.initFields(binding);
        this.updateFieldsValues();

        this.serviceEnabler.setOnClickListener(this::onToggleBlockerClick);
        this.clearCallsLog.setOnClickListener(this::onClearCallsClick);
        this.resetTimer.setOnClickListener(this::onResetTimerClick);
    }

    private void onToggleBlockerClick(View view) {
        SharedPreferences.Editor editor = preferences.edit();

        boolean buttonText = String.valueOf(
                serviceEnabler.getText()
        ).equals(
                String.valueOf(getText(R.string.on_text))
        );

        if (!buttonText) {
            editor.putInt(MainActivity.APP_PREF_STATUS, 0);

            serviceEnabler.setText(R.string.on_text);
            setBooleanFieldValue(blockerStatusText, false);
        } else {
            if (ROLE_GRANTED) {
                if (READ_CONTACTS_GRANTED && CALLS_ACCESS_GRANTED) {
                    editor.putInt(MainActivity.APP_PREF_STATUS, 1);

                    serviceEnabler.setText(R.string.off_text);
                    setBooleanFieldValue(blockerStatusText, true);
                } else {
                    serviceEnabler.setText(R.string.doesnt_have_required_permissions);
                }
            } else {
                serviceEnabler.setText(R.string.doesnt_have_required_role);
            }
        }

        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.updateFieldsValues();
    }

    private void onClearCallsClick(View view) {
        callsHelper.clearCallsLog(getApplicationContext());
        this.clearCallsLog.setText(R.string.cleared);
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                        clearCallsLog.setText(R.string.clear_calls_log),
                1000
        );
    }

    private void onResetTimerClick(View view) {
        PauseParams.getCleared().saveToPreferences(getApplicationContext());

        getApplicationContext().stopService(
                new Intent(getApplicationContext(), WidgetCounterService.class)
        );

        Intent serviceIntent = new Intent(getApplicationContext(), BlockPauseWidget.class);
        serviceIntent.setAction(BlockPauseWidget.RECEIVE_END_COUNT);
        getApplicationContext().sendBroadcast(serviceIntent);

        this.resetTimer.setText(R.string.reseted);
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                        resetTimer.setText(R.string.reset_timer),
                1000
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ROLE_REQUEST_ID) {
            this.ROLE_GRANTED = resultCode == Activity.RESULT_OK;
            this.setBooleanFieldValue(
                    this.bindScreeningServicePermissionValue,
                    resultCode == Activity.RESULT_OK
            );
        }
    }

    private void setBooleanFieldValue(TextView view, Boolean value) {
        view.setText(value ? "True" : "False");
        view.setTextColor(value ? GREEN : RED);
    }

    private void setStringFieldValue(TextView view, String value) {
        view.setText(value);
    }

    private void initPreferences() {
        preferences = this.getSharedPreferences(
                MainActivity.APP_PREF,
                Context.MODE_MULTI_PROCESS
        );

        SharedPreferences.Editor editor = preferences.edit();
        if(!preferences.contains(MainActivity.APP_PREF_STATUS)) {
            editor.putInt(
                    MainActivity.APP_PREF_STATUS, 0
            );
            editor.apply();
        }
        if(!preferences.contains(MainActivity.APP_PREF_LOADED_NUMBERS)) {
            editor.putInt(
                    MainActivity.APP_PREF_LOADED_NUMBERS, 0
            );
            editor.apply();
        }
        if(!preferences.contains(MainActivity.APP_PREF_BLOCKED_CALLS)) {
            editor.putInt(
                    MainActivity.APP_PREF_BLOCKED_CALLS, 0
            );
            editor.apply();
        }
        editor.commit();
    }

    private void initFields(ActivityMainBinding binding) {
        this.serviceEnabler = binding.serviceEnabler;
        this.clearCallsLog = binding.clearCallsLog;
        this.resetTimer = binding.resetTimer;
        this.blockerStatusText = binding.switchValue;
        this.bindScreeningServicePermissionValue = binding.bindScreeningServicePermissionValue;
        this.readContactsPermissionValue = binding.readContactsPermissionValue;
        this.loadedContactsValue = binding.loadedContactsValue;
        this.callsAccessPermissionValue = binding.callsAccessPermissionValue;
        this.blockedCallsValue = binding.blockedCallsValue;

        this.setBooleanFieldValue(this.blockerStatusText, false);
        this.setBooleanFieldValue(this.bindScreeningServicePermissionValue, false);
        this.setBooleanFieldValue(this.readContactsPermissionValue, false);
        this.setBooleanFieldValue(this.callsAccessPermissionValue, false);
    }

    private void updateFieldsValues() {
        this.setStringFieldValue(
                this.loadedContactsValue,
                String.valueOf(preferences.getInt(
                        MainActivity.APP_PREF_LOADED_NUMBERS, 0))
        );

        this.setStringFieldValue(
                this.blockedCallsValue,
                String.valueOf(preferences.getInt(
                        MainActivity.APP_PREF_BLOCKED_CALLS, 0)));

        if(preferences.getInt(MainActivity.APP_PREF_STATUS, 0) != 0) {
            this.setStringFieldValue(
                    this.serviceEnabler,
                    String.valueOf(getText(R.string.off_text))
            );
            this.setBooleanFieldValue(this.blockerStatusText, true);
        }

        READ_CONTACTS_GRANTED = checkReadContactsPermission();
        this.setBooleanFieldValue(this.readContactsPermissionValue, READ_CONTACTS_GRANTED);

        CALLS_ACCESS_GRANTED = this.checkReadCallsPermission();
        this.setBooleanFieldValue(this.callsAccessPermissionValue, CALLS_ACCESS_GRANTED);
    }

    private boolean checkReadCallsPermission() {
        int readCallsPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CALL_LOG);
        int writeCallsPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_CALL_LOG);
        return  (readCallsPermission == PackageManager.PERMISSION_GRANTED) &&
                (writeCallsPermission == PackageManager.PERMISSION_GRANTED);
    }

    private boolean checkReadContactsPermission() {
        int readContactsPermission = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_CONTACTS);
        return readContactsPermission == PackageManager.PERMISSION_GRANTED;
    }
}
package com.kalmar.blockcalls;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.kalmar.blockcalls.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private TextView serviceEnabler, switchValue,
            bindScreeningServicePermissionValue, readContactsPermissionValue,
            loadedContactsValue, blockedCallsValue;

    private static final int ROLE_REQUEST_ID = 1;
    private boolean ROLE_GRANTED = false;
    private boolean READ_CONTACTS_GRANTED = false;

    public static final String APP_PREF = "com.kalmar.BlockCalls.preferences";
    public static final String APP_PREF_ENABLED = "com.kalmar.BlockCalls.enabled";
    public static final String APP_PREF_LOADED_NUMBERS = "com.kalmar.BlockCalls.loaded_numbers";
    public static final String APP_PREF_BLOCKED_CALLS = "com.kalmar.BlockCalls.blocked_calls";

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
        startActivityForResult(intent, ROLE_REQUEST_ID);

        preferences = this.getSharedPreferences(MainActivity.APP_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if(!preferences.contains(MainActivity.APP_PREF_ENABLED)) {
            editor.putInt(
                    MainActivity.APP_PREF_ENABLED, 0
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

        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e){}

        com.kalmar.blockcalls.databinding.ActivityMainBinding binding =
                ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        this.serviceEnabler
                = (TextView) findViewById(R.id.service_enabler);
        this.switchValue
                = (TextView) findViewById(R.id.switch_value);
        this.bindScreeningServicePermissionValue
                = (TextView) findViewById(R.id.bind_screening_service_permission_value);
        this.readContactsPermissionValue
                = (TextView) findViewById(R.id.read_contacts_permission_value);
        this.loadedContactsValue
                = (TextView) findViewById(R.id.loaded_contacts_value);
        this.blockedCallsValue
                = (TextView) findViewById(R.id.blocked_calls_value);


        this.serviceEnabler.setOnClickListener(view -> {
            boolean buttonText = String.valueOf(
                    serviceEnabler.getText()
            ).equals(
                    String.valueOf(getText(R.string.on_text))
            );

            if(!buttonText) {
                stopBlockService();
                serviceEnabler.setText(R.string.on_text);
                setBooleanFieldValue(switchValue, false);
            } else {
                if(ROLE_GRANTED) {
                    if(READ_CONTACTS_GRANTED) {
                        startBlockService();
                        serviceEnabler.setText(R.string.off_text);
                        setBooleanFieldValue(switchValue, true);
                    } else {
                        serviceEnabler.setText(R.string.doesnt_have_required_permissions);
                    }
                } else {
                    serviceEnabler.setText(R.string.doesnt_have_required_role);
                }
            }
        });

        this.setBooleanFieldValue(this.switchValue, false);
        // this.setBooleanFieldValue(this.bindScreeningServiceStatusValue, false);
        this.setBooleanFieldValue(this.bindScreeningServicePermissionValue, false);
        this.setBooleanFieldValue(this.readContactsPermissionValue, false);
        this.setStringFieldValue(
                this.loadedContactsValue,
                String.valueOf(preferences.getInt(
                        MainActivity.APP_PREF_LOADED_NUMBERS, 0)));
        this.setStringFieldValue(
                this.blockedCallsValue,
                String.valueOf(preferences.getInt(
                        MainActivity.APP_PREF_BLOCKED_CALLS, 0)));

        if(preferences.getInt(MainActivity.APP_PREF_ENABLED, 0) != 0) {
            this.setStringFieldValue(
                    this.serviceEnabler,
                    String.valueOf(getText(R.string.off_text))
            );
            this.setBooleanFieldValue(this.switchValue, true);
        }

        int readContactsPermission = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_CONTACTS);
        READ_CONTACTS_GRANTED = readContactsPermission == PackageManager.PERMISSION_GRANTED;
        this.setBooleanFieldValue(
                this.readContactsPermissionValue,
                READ_CONTACTS_GRANTED
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

    private void startBlockService() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(MainActivity.APP_PREF_ENABLED, 1);
        editor.apply();

        this.setBooleanFieldValue(this.switchValue, true);
    }

    private void stopBlockService() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(MainActivity.APP_PREF_ENABLED, 0);
        editor.apply();

        this.setBooleanFieldValue(this.switchValue, false);
    }

    private void setBooleanFieldValue(TextView view, Boolean value) {
        view.setText(value ? "True" : "False");
        view.setTextColor(value ? GREEN : RED);
    }

    private void setStringFieldValue(TextView view, String value) {
        view.setText(value);
    }
}
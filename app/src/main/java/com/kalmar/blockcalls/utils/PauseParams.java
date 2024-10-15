package com.kalmar.blockcalls.utils;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class PauseParams {
    public static final List<Integer> COUNTER_STEPS = Arrays.asList(
            5 * 60, 15 * 60, 30 * 60, 60 * 60
    );
    public static final String PAUSE_PREF =
            "com.kalmar.BlockCalls.pausePreferences";
    public static final String PAUSE_PREF_START_TIME =
            "com.kalmar.BlockCalls.pausePreferences.startTime";
    public static final String PAUSE_PREF_END_TIME =
            "com.kalmar.BlockCalls.endTime";
    public static final String PAUSE_PREF_CURRENT_STEP =
            "com.kalmar.BlockCalls.pausePreferences.currentStep";
    private long startTime = -1;
    private long endTime = -1;
    private int currentStep = -1;

    public PauseParams(long startTime, long endTime, int currentStep) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentStep = currentStep;
    }

    public boolean isPaused() {
        long currentUnixTime = Calendar.getInstance().getTime().getTime() / 1000;

        return this.endTime > currentUnixTime;
    }

    public void increaseStep() {
        long currentUnixTime = Calendar.getInstance().getTime().getTime() / 1000;

        Log.i(TAG, String.valueOf(this.startTime));
        Log.i(TAG, String.valueOf(this.endTime));
        Log.i(TAG, String.valueOf(this.currentStep));

        if(this.endTime < currentUnixTime) {
            this.startTime = currentUnixTime;
            this.endTime = currentUnixTime + COUNTER_STEPS.get(0);
            this.currentStep = COUNTER_STEPS.get(0);
        } else {
            int counterStepIndex = COUNTER_STEPS.indexOf(this.currentStep);

            if(counterStepIndex < COUNTER_STEPS.size() - 1) {
                this.endTime = this.startTime + COUNTER_STEPS.get(counterStepIndex + 1);
                this.currentStep = COUNTER_STEPS.get(counterStepIndex + 1);
            }
        }
    }

    public int getRemainingMinutes() {
        long currentUnixTime = Calendar.getInstance().getTime().getTime() / 1000;

        return (int) ((this.endTime - currentUnixTime) / 60);
    }

    public int getCurrentProgress() {
        long currentUnixTime = Calendar.getInstance().getTime().getTime() / 1000;

        return (int) (((this.endTime - currentUnixTime) / (this.currentStep * 1.0)) * 100);
    }

    static public PauseParams getFromPreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PauseParams.PAUSE_PREF,
                Context.MODE_MULTI_PROCESS
        );


        long startTime = preferences.getLong(
                PauseParams.PAUSE_PREF_START_TIME,
                -1
        );
        long endTime = preferences.getLong(
                PauseParams.PAUSE_PREF_END_TIME,
                -1
        );
        int currentStep = preferences.getInt(
                PauseParams.PAUSE_PREF_CURRENT_STEP,
                -1
        );

        return new PauseParams(startTime, endTime, currentStep);
    }

    static public PauseParams getCleared() {
        return new PauseParams(-1, -1, -1);
    }

    public void saveToPreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PauseParams.PAUSE_PREF,
                Context.MODE_MULTI_PROCESS
        );

        SharedPreferences.Editor editor = preferences.edit();

        editor.putLong(
                PauseParams.PAUSE_PREF_START_TIME,
                startTime
        );
        editor.putLong(
                PauseParams.PAUSE_PREF_END_TIME,
                endTime
        );
        editor.putInt(
                PauseParams.PAUSE_PREF_CURRENT_STEP,
                currentStep
        );

        editor.commit();
    }
}

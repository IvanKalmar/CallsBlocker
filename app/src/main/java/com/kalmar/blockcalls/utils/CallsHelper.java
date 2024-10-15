package com.kalmar.blockcalls.utils;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CallsHelper {

    public String processNumber(String number) {
        return number.replaceAll("%2B", "")
                .replaceAll("tel:", "")
                .replaceAll("\\D", "");
    }

    @SuppressLint("Range")
    public HashSet<String> getNumbers(Context context) {
        HashSet<String> numbers = new HashSet<String>();

        ContentResolver cr = context.getContentResolver();
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
                        numbers.add(this.processNumber(pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )));
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

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    public void clearCallsLog(Context context) {
        HashSet<String> contactNumbers = this.getNumbers(context);
        List<String> callsLog = this.getCallsLog(context).stream().toList();
        float callsLogSize = callsLog.size();

        if (callsLogSize > 0) {
            for(int i = 0; i < callsLog.size(); i++) {
                if(!contactNumbers.contains(this.processNumber(callsLog.get(i)))) {
                    context.getContentResolver().delete(
                            CallLog.Calls.CONTENT_URI,
                            "number LIKE '%" + callsLog.get(i) + "%'",
                            null
                    );
                }
            }
        }
    }

    private Set<String> getCallsLog(Context context) {
        Set<String> calls = new HashSet<>();

        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(
                CallLog.Calls.CONTENT_URI,
                null, null, null, null
        );

        int totalCall = 1;

        if (c != null) {
            totalCall = 10;

            if (c.moveToLast()) {
                for (int j = 0; j < totalCall; j++) {
                    try {
                        String phNumber = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                        calls.add(phNumber);
                        c.moveToPrevious();
                    } catch (Exception ignored) { }
                }
            }
            c.close();
        }

        return calls;
    }
}

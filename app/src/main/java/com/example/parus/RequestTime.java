package com.example.parus;

import android.os.AsyncTask;

import com.google.firebase.Timestamp;

import org.apache.commons.net.time.TimeTCPClient;

import java.io.IOException;

public class RequestTime extends AsyncTask<String, String, Timestamp> {

    @Override
    protected void onPostExecute(Timestamp s) {
        super.onPostExecute(s);
    }

    @Override
    protected Timestamp doInBackground(String... uri) {
        try {
            TimeTCPClient client = new TimeTCPClient();
            try {
                client.setDefaultTimeout(3000);
                client.connect("time.nist.gov");
                return new Timestamp(client.getDate());
            } finally {
                client.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

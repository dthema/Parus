package com.example.parus;

import android.os.AsyncTask;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import java.net.InetAddress;

public class RequestTime extends AsyncTask<String, String, TimeStamp> {
    private static final String TIME_SERVER = "time-a.nist.gov";

    @Override
    protected void onPostExecute(TimeStamp s) {
        super.onPostExecute(s);
    }

    @Override
    protected TimeStamp doInBackground(String... uri) {
        try {
            NTPUDPClient timeClient = new NTPUDPClient();
            InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
            TimeInfo timeInfo = timeClient.getTime(inetAddress);
            return timeInfo.getMessage().getTransmitTimeStamp();
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
//        try {
//            TimeTCPClient client = new TimeTCPClient();
//            try {
//                client.setDefaultTimeout(3000);
//                client.connect("time.nist.gov");
//                return client.getDate();
//            } finally {
//                client.disconnect();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null
//        }
    }


}

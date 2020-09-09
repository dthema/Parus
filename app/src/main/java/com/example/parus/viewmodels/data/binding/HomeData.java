package com.example.parus.viewmodels.data.binding;

import androidx.databinding.BaseObservable;

public class HomeData extends BaseObservable {

    private String currentReminder;
    private String heartRate;
    private String linkUserOnline;
    private String fastAction;

    public HomeData(){}

    public String getCurrentReminder() {
        return currentReminder;
    }

    public void setCurrentReminder(String currentReminder) {
        this.currentReminder = currentReminder;
        notifyChange();
    }

    public String getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(String heartRate) {
        this.heartRate = heartRate;
        notifyChange();
    }

    public String getLinkUserOnline() {
        return linkUserOnline;
    }

    public void setLinkUserOnline(String linkUserOnline) {
        this.linkUserOnline = linkUserOnline;
        notifyChange();
    }

    public String getFastAction() {
        return fastAction;
    }

    public void setFastAction(String fastAction) {
        this.fastAction = fastAction;
        notifyChange();
    }
}

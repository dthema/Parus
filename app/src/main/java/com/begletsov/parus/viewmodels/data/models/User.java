package com.begletsov.parus.viewmodels.data.models;

import com.google.firebase.Timestamp;

import java.util.HashMap;

public class User {

    private String userId;
    private String linkUserId;
    private String token;
    private String name;
    private String fastAction;
    private boolean support;
    private boolean checkGeoPosition;
    private boolean checkHeartBPM;
    private HashMap<String, Object> SaySettings;
    private HashMap<String, Object> WordsOften;
    private Timestamp lastOnline;
    private Long pulse;

    public User(){}

    public User(String userId, String linkUserId, String token, String name, String fastAction, boolean support, boolean checkGeoPosition, boolean checkHeartBPM, HashMap<String, Object> saySettings, HashMap<String, Object> wordsOften, Timestamp lastOnline, Long pulse) {
        this.userId = userId;
        this.linkUserId = linkUserId;
        this.token = token;
        this.name = name;
        this.fastAction = fastAction;
        this.support = support;
        this.checkGeoPosition = checkGeoPosition;
        this.checkHeartBPM = checkHeartBPM;
        SaySettings = saySettings;
        WordsOften = wordsOften;
        this.lastOnline = lastOnline;
        this.pulse = pulse;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLinkUserId() {
        return linkUserId;
    }

    public void setLinkUserId(String linkUserId) {
        this.linkUserId = linkUserId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFastAction() {
        return fastAction;
    }

    public void setFastAction(String fastAction) {
        this.fastAction = fastAction;
    }

    public boolean isSupport() {
        return support;
    }

    public void setSupport(boolean support) {
        this.support = support;
    }

    public boolean isCheckGeoPosition() {
        return checkGeoPosition;
    }

    public void setCheckGeoPosition(boolean checkGeoPosition) {
        this.checkGeoPosition = checkGeoPosition;
    }

    public boolean isCheckHeartBPM() {
        return checkHeartBPM;
    }

    public void setCheckHeartBPM(boolean checkHeartBPM) {
        this.checkHeartBPM = checkHeartBPM;
    }

    public HashMap<String, Object> getSaySettings() {
        return SaySettings;
    }

    public void setSaySettings(HashMap<String, Object> saySettings) {
        SaySettings = saySettings;
    }

    public HashMap<String, Object> getWordsOften() {
        return WordsOften;
    }

    public void setWordsOften(HashMap<String, Object> wordsOften) {
        WordsOften = wordsOften;
    }

    public Timestamp getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(Timestamp lastOnline) {
        this.lastOnline = lastOnline;
    }

    public Long getPulse() {
        return pulse;
    }

    public void setPulse(Long pulse) {
        this.pulse = pulse;
    }
}

package com.begletsov.parus.viewmodels.data.models;

import androidx.annotation.Nullable;

import java.util.Date;

public class Chat {

    private String id;
    private String sender;
    private String receiver;
    private String message;
    private Date date;
    private boolean isCalendar;

    public Chat(String sender, String receiver, String message, Date date, boolean fromSupport) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.date = date;
    }

    public Chat() {
        this.sender = "";
        this.receiver = "";
        this.message = "";
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    private String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isCalendar() {
        return isCalendar;
    }

    public void setCalendar(boolean calendar) {
        isCalendar = calendar;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!obj.getClass().equals(this.getClass())) return false;
        Chat that = (Chat) obj;
        return this.getMessage().equals(that.getMessage()) &&
                this.getSender().equals(that.getSender()) &&
                this.getReceiver().equals(that.getReceiver()) &&
                this.getDate() == that.getDate();
    }}

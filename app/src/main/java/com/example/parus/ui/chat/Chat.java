package com.example.parus.ui.chat;

import java.util.Date;

public class Chat {

    private String sender;
    private String receiver;
    private String message;
    private Date date;
    private boolean fromSupport;

    public Chat(String sender, String receiver, String message, Date date, boolean fromSupport) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.date = date;
        this.fromSupport = fromSupport;
    }

    public Chat() {
        this.sender = "";
        this.receiver = "";
        this.message = "";
    }

    public boolean isFromSupport() {
        return fromSupport;
    }

    public void setFromSupport(boolean fromSupport) {
        this.fromSupport = fromSupport;
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

    public String getReceiver() {
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

}

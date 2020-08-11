package com.example.parus.viewmodels.data.models;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class Reminder {

    private String name;
    private Integer type;
    private List<Date> timers;
    private Date timeStart;
    private Date timeEnd;
    private Date timeInterval;
    private Date timeCreate;
    private String id;

    public Reminder(){}

    public Reminder(String name, Integer type, List<Date> timers, Date timeStart, Date timeEnd, Date timeInterval, Date timeCreate) {
        this.name = name;
        this.type = type;
        this.timers = timers;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.timeInterval = timeInterval;
        this.timeCreate = timeCreate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getTimeCreate() {
        return timeCreate;
    }

    public void setTimeCreate(Date timeCreate) {
        this.timeCreate = timeCreate;
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<Date> getTimers() {
        return timers;
    }

    public void setTimers(List<Date> timers) {
        this.timers = timers;
    }

    public Date getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Date timeStart) {
        this.timeStart = timeStart;
    }

    public Date getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Date timeEnd) {
        this.timeEnd = timeEnd;
    }

    public Date getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(Date timeInterval) {
        this.timeInterval = timeInterval;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!obj.getClass().equals(this.getClass())) return false;
        Reminder that = (Reminder) obj;
        return this.getId().equals(that.getId()) &&
                this.getName().equals(that.getName()) &&
                this.getType().equals(that.getType()) &&
                this.getTimers() == that.getTimers() &&
                this.getTimeCreate() == that.getTimeCreate() &&
                this.getTimeStart() == that.getTimeStart() &&
                this.getTimeInterval() == that.getTimeInterval() &&
                this.getTimeEnd() == that.getTimeEnd();
    }
}

package com.yg.amit;

public class Meeting {

    private String person, date, time;

    public Meeting(String person, String date, String time) {
        this.person = person;
        this.date = date;
        this.time = time;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}

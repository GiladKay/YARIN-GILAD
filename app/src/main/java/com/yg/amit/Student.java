package com.yg.amit;

public class Student {
    private String name;
    private int meetingCount;
    //TODO more stats? email current planned meetings etc...

    public Student(String name,int meetingCount){
        this.name=name;
        this.meetingCount=meetingCount;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMeetingCount() {
        return meetingCount;
    }

    public void setMeetingCount(int meetingCount) {
        this.meetingCount = meetingCount;
    }

}

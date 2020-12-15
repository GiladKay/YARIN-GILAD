package com.yg.amit;

public class Teacher {
    private String name;
    private int meetCount;
    //TODO add teacher stats
    public Teacher(String name,int meetCount){
        this.name=name;
        this.meetCount=meetCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMeetCount() {
        return meetCount;
    }

    public void setMeetCount(int meetCount) {
        this.meetCount = meetCount;
    }

    public void inc(){
        this.meetCount++;
    }
}

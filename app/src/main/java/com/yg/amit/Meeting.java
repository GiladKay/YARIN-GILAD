package com.yg.amit;

public class Meeting {

    private String teacher,student, date, time;

    public Meeting(String student,String teacher, String date, String time) {
        this.teacher=teacher;
        this.student=student;
        this.date = date;
        this.time = time;
    }

    public String getTeacher() { return teacher; }


    public String getStudent() { return student; }

    public String getFileName(){
        return student+"&"+teacher+".txt";
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

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

    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getStudent() { return student; }

    public void setStudent(String student) { this.student = student; }

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

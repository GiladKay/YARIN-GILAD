package com.yg.amit;

public class Class {


    private String className;

    public Class(String className) {

        this.className = className.substring(0,className.length()-4);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}

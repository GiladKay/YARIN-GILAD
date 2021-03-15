package com.yg.amit;

import android.content.Context;
import android.util.Log;

public class Utils {

    //for sending emails (sender not receiver)
    public static final String EMAIL = "meetingsamit@gmail.com";//
    public static final String PASSWORD = "ysgk12345";//

    //SharedPreference keys
    public static final String AMIT_SP = "AMIT"; // key for shared preference
    public static final String NAME_KEY = "name";// key for name of user
    public static final String TYPE_KEY = "type";// key for Access type of user

    // log tag
    public static final String TAG = "debug_tag";


    // the modes in which a user can view meeting (upcoming meetings, done meetings (with teacher mashov) and
    // finished meeting with both teacher and student mashov)
    public static final int MODE_UPCOMING = 0;//
    public static final int MODE_DONE = 1;//
    public static final int MODE_FINISHED = 2;//

    // key for the Intent Extra that contains the class name
    public static final String CLASS_NAME_KEY = "class";//

    // Access types
    public static final String TYPE_STUDENT = "student";
    public static final String TYPE_TEACHER = "teacher";
    public static final String TYPE_ADMIN = "admin";

    // key for sp containing the last state of the "add to calendar" switch
    public static final String SWITCH_STATE = "switchState";//

    // keys connected to getting calendar permissions
    public static final int STORAGE_PERMISSION_CODE1 = 1;
    public static final int STORAGE_PERMISSION_CODE2 = 2;

    // extra keys for intents
    public static final String KEY_STUDENT_NAME= "SName"; //get student name to and from MeetingActivity
    public static final String KEY_MEETING_COUNT = "mCount"; // get student meet count to and from MeetingActivity
    public static final String KEY_CLASS_NAME = "classname"; // get student class name to and from MeetingActivity
    public static final String KEY_PREVIOUS_ACTIVITY = "pActivity"; // get the name of the activity that sent the user to MeetingActivity
    public static final String KEY_MODE = "Mode"; // get the mode the user was currently on when clicking on meeting (Upcoming, Done,Finished)
    public static final String KEY_FILE_NAME = "Meeting"; //get the name of the meeting file

    // Meeting Origin (the two extra options for KEY_PREVIOUS_ACTIVITY)
    public static final String ORG_MEETINGS= "Meetings";
    public static final String ORG_INFO = "Info";


    /**
     * sends emails
     *
     * @param address = email address of recipient
     * @param subject = title of email
     * @param message = contents of email
     */
    public static void sendEmail(Context context, String address, String subject, String message) {
        javaMailAPI javaMailAPI = new javaMailAPI(context, address, subject, message);
        javaMailAPI.execute();

    }


}

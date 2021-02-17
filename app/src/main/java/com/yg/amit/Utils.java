package com.yg.amit;

public class Utils {

    //for sending emails (sender not receiver)
    public static final String EMAIL = "meetingsamit@gmail.com";//
    public static final String PASSWORD = "ysgk12345";//

    //SharedPreference keys
    public static final String AMIT_SP = "AMIT"; //
    public static final String NAME_KEY = "name";//
    public static final String TYPE_KEY = "type";//

    // log tag
    public static final String TAG="TAG";//


    // the modes in which a user can view meeting (upcoming meetings, done meetings (with teacher mashov) and
    // finished meeting with both teacher and student mashov)
    public static final int MODE_UPCOMING=0;//
    public static final int MODE_DONE=1;//
    public static final int MODE_FINISHED=2;//

    // key for the Intent Extra that contains the class name
    public static final String CLASS_NAME_KEY = "class";//



    // key for sp containing the last state of the "add to calendar" switch
    public static final String SWITCH_STATE = "switchState";//


    public static final int STORAGE_PERMISSION_CODE = 1;
}

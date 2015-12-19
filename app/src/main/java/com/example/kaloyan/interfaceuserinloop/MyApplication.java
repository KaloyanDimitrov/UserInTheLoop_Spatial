package com.example.kaloyan.interfaceuserinloop;

import android.app.Application;


public class MyApplication extends Application {

    private static MyApplication application;


    private int mode;
    private boolean shareData;


    public MyApplication getInstance(){
        return application;
    }

    private void init(){
        mode = 0;
        shareData = true;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        application = this;
        application.init();
    }

    public int getMode(){
        return mode;
    }

    public void setMode(int mode){
        this.mode = mode;
    }

    public boolean getShareData(){
        return shareData;
    }

    public void setShareData(boolean shareData){
        this.shareData = shareData;
    }

}

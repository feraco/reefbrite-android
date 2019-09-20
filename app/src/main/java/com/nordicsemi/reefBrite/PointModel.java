package com.nordicsemi.reefBrite;

import android.support.annotation.NonNull;

public class PointModel implements Comparable{
    private int hour;
    private int minu;
    private int blueV;
    private int whiteV;
    private String time;
    private String bluePercent;
    private String whitePercent;

    public PointModel(){

    }

    public PointModel(int hour, int minu, int blueV, int whiteV) {
        this.hour = hour;
        this.minu = minu;
        this.blueV = blueV;
        this.whiteV = whiteV;
        calTime();
        calPercent();
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {

        this.hour = hour;
        calTime();
    }

    public int getMinu() {
        return minu;
    }

    public void setMinu(int minu) {

        this.minu = minu;
        calTime();
    }

    public int getBlueV() {
        return blueV;
    }

    public void setBlueV(int blueV) {

        this.blueV = blueV;
        calPercent();
    }

    public int getWhiteV() {
        return whiteV;
    }

    public void setWhiteV(int whiteV) {

        this.whiteV = whiteV;
        calPercent();
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getBluePercent() {
        return bluePercent;
    }

    public void setBluePercent(String bluePercent) {
        this.bluePercent = bluePercent;
    }

    public String getWhitePercent() {
        return whitePercent;
    }

    public void setWhitePercent(String whitePercent) {
        this.whitePercent = whitePercent;
    }

    public void calTime(){
        String minuT = "";
        if(this.minu<10){
            minuT = "0" + minu;
        }else{
            minuT = minu + "";
        }
        String hourT = "";
        if(this.hour<12){
            if(this.hour==0){
                hourT = "12";
            }else{
                hourT = "" + this.hour;
            }
            this.time = hourT + ":" + minuT + "AM";
        }else{
            if(this.hour==12){
                hourT = "12";
            }else{
                hourT = "" + (this.hour - 12);
            }
            this.time = hourT + ":" + minuT + "PM";
        }
    }

    public void calPercent(){
        this.bluePercent = (int)(blueV/2.55) + "%";
        this.whitePercent = (int)(whiteV/2.55) + "%";
    }

    public int getTimeInMinu(){
        return (this.hour*60)+this.minu;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return this.getTimeInMinu()-((PointModel)o).getTimeInMinu();
    }
}

package com.example.smarthomeapplication;

public class Device_Switch_Control {
    public String Light_Switch(boolean isChecked) {
        String status = String.format("%s",isChecked ?"T":"F");;
        String payload = "{\"Device\":\"Light\",\"Status\":\"" + status + "\"}";
        return payload;
    }
    public String Fan_Switch(boolean isChecked) {
        String status = String.format("%s",isChecked ?"T":"F");;
        String payload = "{\"Device\":\"Fan\",\"Status\":\"" + status + "\"}";
        return payload;
    }
    public String Humidifier_Switch(boolean isChecked) {
        String status = String.format("%s",isChecked ?"T":"F");;
        String payload = "{\"Device\":\"Humidifier\",\"Status\":\"" + status + "\"}";
        return payload;
    }

}

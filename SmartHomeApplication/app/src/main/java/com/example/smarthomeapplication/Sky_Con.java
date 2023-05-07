package com.example.smarthomeapplication;

public class Sky_Con {
    public String SkyCon_Switch(String skycon) {
        String returnload = "0";
        switch (skycon){
            case "CLEAR_DAY": returnload = "晴（白天）";
                break;
            case "CLEAR_NIGHT": returnload = "晴（夜间）";
                break;
            case "PARTLY_CLOUDY_DAY": returnload = "多云（白天）";
                break;
            case "PARTLY_CLOUDY_NIGHT": returnload = "多云（夜间）";
                break;
            case "CLOUDY": returnload = "阴";
                break;
            case "LIGHT_HAZE": returnload = "轻度雾霾";
                break;
            case "MODERATE_HAZE": returnload = "中度雾霾";
                break;
            case "HEAVY_HAZE": returnload = "重度雾霾";
                break;
            case "LIGHT_RAIN": returnload = "小雨";
                break;
            case "MODERATE_RAIN": returnload = "中雨";
                break;
            case "HEAVY_RAIN": returnload = "大雨";
                break;
            case "STORM_RAIN": returnload = "暴雨";
                break;
            case "FOG": returnload = "雾";
                break;
            case "LIGHT_SNOW": returnload = "小雪";
                break;
            case "MODERATE_SNOW": returnload = "中雪";
                break;
            case "HEAVY_SNOW": returnload = "大雪";
                break;
            case "STORM_SNOW": returnload = "暴雪";
                break;
            case "DUST": returnload = "浮尘";
                break;
            case "SAND": returnload = "沙尘";
                break;
            case "WIND": returnload = "大风";
                break;
            default: returnload = "error";
        }
        return returnload;
    }
}

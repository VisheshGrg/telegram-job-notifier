package com.telegram_notifier.util;

public final class TextUtils{
    private TextUtils(){}

    public static String truncate(String s, int max){
        if(s==null) return null;
        return s.length() <= max ? s : s.substring(0,max-3) + "...";
    }
}
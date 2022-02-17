package com.example.ffmpeg.dto;

import java.util.List;

public class VideoRecord {

    private String date;

    private List<String> list;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

}
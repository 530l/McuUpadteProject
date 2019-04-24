package com.mcuupdate.model;



public class MenuData {
    private String title1;
    private String title2;
    private Object value;

    public MenuData(String title1, String title2, Object value) {
        this.title1 = title1;
        this.title2 = title2;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getTitle1() {
        return title1;
    }

    public void setTitle1(String title1) {
        this.title1 = title1;
    }

    public String getTitle2() {

        return value == null ? "未选择" : title2;
    }

    public void setTitle2(String title2) {
        this.title2 = title2;
    }
}

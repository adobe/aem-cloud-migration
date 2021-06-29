package com.adobe.skyline.migration.model;

public class VideoProfileConfig extends RenditionConfig{
    private int bitRate;
    private String codec;

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }
}

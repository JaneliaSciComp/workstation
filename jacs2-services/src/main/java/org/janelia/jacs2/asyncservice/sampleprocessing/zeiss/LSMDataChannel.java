package org.janelia.jacs2.asyncservice.sampleprocessing.zeiss;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LSMDataChannel {
    @JsonProperty("DATACHANNEL_ENTRY_ACQUIRE")
    private String acquire;
    @JsonProperty("DATACHANNEL_ENTRY_BITSPERSAMPLE")
    private String bitsPerSample;
    @JsonProperty("DATACHANNEL_ENTRY_COLOR")
    private Long color;
    @JsonProperty("DATACHANNEL_ENTRY_NAME")
    private String name;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_CHANNEL1")
    private Double ratioChannel1;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_CHANNEL2")
    private Double ratioChannel2;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_CONST1")
    private Double rationConst1;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_CONST2")
    private Double rationConst2;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_CONST3")
    private Double rationConst3;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_CONST4")
    private Double rationConst4;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_CONST5")
    private Double rationConst5;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_CONST6")
    private Double rationConst6;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_TRACK1")
    private String ratioTrack1;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_TRACK2")
    private String ratioTrack2;
    @JsonProperty("DATACHANNEL_ENTRY_RATIO_TYPE")
    private String ratioType;
    @JsonProperty("DATACHANNEL_ENTRY_SAMPLETYPE")
    private String sampleType;
    @JsonProperty("DATACHANNEL_MMF_INDEX")
    private String mmfIndex;

    public String getAcquire() {
        return acquire;
    }

    public String getBitsPerSample() {
        return bitsPerSample;
    }

    public Long getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public Double getRatioChannel1() {
        return ratioChannel1;
    }

    public Double getRatioChannel2() {
        return ratioChannel2;
    }

    public Double getRationConst1() {
        return rationConst1;
    }

    public Double getRationConst2() {
        return rationConst2;
    }

    public Double getRationConst3() {
        return rationConst3;
    }

    public Double getRationConst4() {
        return rationConst4;
    }

    public Double getRationConst5() {
        return rationConst5;
    }

    public Double getRationConst6() {
        return rationConst6;
    }

    public String getRatioTrack1() {
        return ratioTrack1;
    }

    public String getRatioTrack2() {
        return ratioTrack2;
    }

    public String getRatioType() {
        return ratioType;
    }

    public String getSampleType() {
        return sampleType;
    }

    public String getMmfIndex() {
        return mmfIndex;
    }
}

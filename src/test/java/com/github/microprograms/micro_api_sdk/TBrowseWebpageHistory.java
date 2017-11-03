package com.github.microprograms.micro_api_sdk;

public class TBrowseWebpageHistory {
    private Integer id;
    private String uuid;
    private String url;
    private String title;
    private Long browseTimestamp;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getBrowseTimestamp() {
        return browseTimestamp;
    }

    public void setBrowseTimestamp(Long browseTimestamp) {
        this.browseTimestamp = browseTimestamp;
    }
}

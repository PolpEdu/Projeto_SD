package com.ProjetoSD.web.Models;

public class IndexRequest {
    private String url;

    public IndexRequest() {
    }

    public IndexRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

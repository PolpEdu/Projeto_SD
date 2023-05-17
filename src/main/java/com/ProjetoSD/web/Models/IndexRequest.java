package com.ProjetoSD.web.Models;

/** This class represents a index request. */
public class IndexRequest {
    /** The url. */
    private String url;
    /** Default constructor. */
    public IndexRequest() {
    }
    /**
     * Constructor.
     *
     * @param url The url.
     */
    public IndexRequest(String url) {
        this.url = url;
    }

    /**
     * Gets the url.
     *
     * @return The url.
     */
    public String getUrl() {
        return this.url;
    }
    /**
     * Sets the url.
     *
     * @param url The url.
     */
    public void setUrl(String url) {
        this.url = url;
    }
}

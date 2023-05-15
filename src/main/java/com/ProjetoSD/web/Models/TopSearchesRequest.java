package com.ProjetoSD.web.Models;

import java.util.ArrayList;

public class TopSearchesRequest {
    private ArrayList<String> topSearches;

    public TopSearchesRequest() {

    }

    public TopSearchesRequest(ArrayList<String> topSearches) {
        this.topSearches = topSearches;
    }

    public ArrayList<String> getTopSearches() {
        return this.topSearches;
    }

    public void setTopSearches(ArrayList<String> topSearches) {
        this.topSearches = topSearches;
    }
}

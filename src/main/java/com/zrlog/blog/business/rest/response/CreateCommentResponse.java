package com.zrlog.blog.business.rest.response;

public class CreateCommentResponse {

    public CreateCommentResponse() {
    }

    public CreateCommentResponse(String alias) {
        this.alias = alias;
    }

    private String alias;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}

package com.shoes.ecommerce.dto;

public class ChatResponse {
    private String reply;
    private String error;

    public ChatResponse() {}
    public ChatResponse(String reply, String error){ this.reply = reply; this.error = error; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}

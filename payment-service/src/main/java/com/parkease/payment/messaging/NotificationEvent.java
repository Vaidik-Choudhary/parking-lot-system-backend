package com.parkease.payment.messaging;

import java.io.Serializable;

public class NotificationEvent implements Serializable {

    private String recipientEmail;
    private String type;
    private String channel = "BOTH";
    private String title;
    private String message;
    private Long   relatedId;
    private String relatedType;

    public NotificationEvent() {
        // Empty constructor required for serialization
    }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }

    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String relatedType) { this.relatedType = relatedType; }

    public static NotificationEventBuilder builder() {
        return new NotificationEventBuilder();
    }

    public static class NotificationEventBuilder {
        private final NotificationEvent instance = new NotificationEvent();
        public NotificationEventBuilder recipientEmail(String e) { instance.setRecipientEmail(e); return this; }
        public NotificationEventBuilder type(String t) { instance.setType(t); return this; }
        public NotificationEventBuilder channel(String c) { instance.setChannel(c); return this; }
        public NotificationEventBuilder title(String t) { instance.setTitle(t); return this; }
        public NotificationEventBuilder message(String m) { instance.setMessage(m); return this; }
        public NotificationEventBuilder relatedId(Long id) { instance.setRelatedId(id); return this; }
        public NotificationEventBuilder relatedType(String t) { instance.setRelatedType(t); return this; }
        public NotificationEvent build() { return instance; }
    }
}

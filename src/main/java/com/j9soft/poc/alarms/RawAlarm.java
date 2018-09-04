package com.j9soft.poc.alarms;

class RawAlarm {
    private String notificationIdentifier;
    private String value; // JSON with attributes

    RawAlarm(String notificationIdentifier, String value) {
        this.notificationIdentifier = notificationIdentifier;
        this.value = value;
    }

//    public String getNotificationIdentifier() {
//        return this.notificationIdentifier;
//    }

    public String getValue() {
        return this.value;
    }
}

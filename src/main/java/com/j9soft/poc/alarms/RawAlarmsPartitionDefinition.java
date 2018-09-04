package com.j9soft.poc.alarms;

class RawAlarmsPartitionDefinition {
    private String domain;
    private String adapterName;

    RawAlarmsPartitionDefinition(String domain, String adapterName) {
        this.domain = domain;
        this.adapterName = adapterName;
    }

    String getAdapterName() {
        return this.adapterName;
    }

    String getDomain() {
        return this.domain;
    }

}

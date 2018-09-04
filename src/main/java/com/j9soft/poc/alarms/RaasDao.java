package com.j9soft.poc.alarms;

/**
 * Interface defining what functionality is provided by a data access layer.
 */
public interface RaasDao {

    void createOrUpdateAlarm(String domain, String adapterName, String notificationIdentifier, String value);

    RawAlarmsPack queryAlarms(String domain, String adapterName, String subpartitionName, String tagOfTheFirstAlarmToBeReturned, int howMany);

    String[] getSubpartitions(RawAlarmsPartitionDefinition partitionDefinition);
}

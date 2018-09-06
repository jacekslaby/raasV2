package com.j9soft.poc.alarms;

/**
 * Interface defining functionality provided by API (version 2) of Raw Active Alarms Store.
 */
public interface RaasV2 {

    /**
     * Retrieve a pack of alarms.
     * Using this method it is possible to retrieve consistent packs of alarms regardless of incoming changes in alarms.
     * (I.e. With this method it is possible to use pagination in client code.) 
     *
     * The first pack is retrieved with tagOfTheFirstAlarmToBeReturned set to null.
     * The next pack is retrieved with tagOfTheFirstAlarmToBeReturned set to the TagOfTheNextAvailableAlarm returned by the first invocation.
     * If returned moreAvailable is false then it means that currently there are no more alarms in this subpartition.
     *
     * It may happen that an alarm is removed in between invocations of this method.
     * Such alarm is provided again in another pack and it contains null as its value.
     * (So only the first pack is guaranteed not to contain null values.)
     * (And bear in mind that any subsequent pack may contain an alarm already delivered in a previous pack -
     * the only guarantee is that it does not happen within a pack.)
     * This way clients are able to rebuild a consistent list of all alarms. (which may come and go while a client
     * is in between retrievals of next packs.)
     *
     * If time between the beginning of retrieval of the first pack and the end of the last pack (the one with moreAvailable=false) is more than ONE hour
     * then it is possible that obsolete alarms (i.e. already removed) retrieved in the previous packs will not have "nullify" value in the following packs,
     * i.e. that these obsolete alarms cannot be distinguished by a client.
     *
     * @param subpartitionName
     * @param tagOfTheFirstAlarmToBeReturned if null then the first pack is returned
     * @return struct(String,tagOfTheNextAvailableAlarm,moreAvailable)
     */
    RawAlarmsPack getRawAlarms(RawAlarmsPartitionDefinition partitionDefinition,
                                String subpartitionName, String tagOfTheFirstAlarmToBeReturned, int howMany);

    /**
     * Retrieve names of subpartitions available in the specified partition.
     * (Note: Alarms from a big adapter may be kept in several subpartitions in order to facilitate concurrent processing.)
     */
    String[] getRawAlarmsSubpartitions(RawAlarmsPartitionDefinition partitionDefinition);

    /**
     * Create or Replace an alarm. (if replacing then all not provided attributes are nullified)
     * (Note: providing null valueObjectAsJson is equivalent to {@link #deleteAlarm(String,RawAlarmsPartitionDefinition)}.)
     */
    void putRawAlarm(String notificationIdentifier, RawAlarmsPartitionDefinition partitionDefinition,
                     String valueObjectAsJson);

    /**
     * Delete an alarm.
     */
    void deleteAlarm(String notificationIdentifier, RawAlarmsPartitionDefinition partitionDefinition);

}

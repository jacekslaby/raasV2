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
     */
    void putRawAlarm(String notificationIdentifier, RawAlarmsPartitionDefinition partitionDefinition,
                     String valueObjectAsJson);

}

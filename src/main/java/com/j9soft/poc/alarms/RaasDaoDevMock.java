package com.j9soft.poc.alarms;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of data access layer (DAO) based on a hashmap kept in memory.
 *
 * This Dao is used in dev mode, i.e. in development environments.
 * (BTW: In prod (i.e. production) mode a different Dao is used. One which connects to a real DB.)
 */
@Profile("default")
@Service
public class RaasDaoDevMock implements RaasDao {

    private static final String THE_ONLY_SUBPARTITION = "33";

    private final Map<String, String> inMemoryMap = new TreeMap<>();
    private final JSONValuePatchingComponent patcher = new JSONValuePatchingComponent();

    @Override
    public void createOrUpdateAlarm(String domain, String adapterName, String notificationIdentifier, String value) {
        synchronized (inMemoryMap) {

            if (value != null) {
                inMemoryMap.put(notificationIdentifier, value);
            } else {
                inMemoryMap.remove(notificationIdentifier);
            }

        }
    }

    @Override
    public RawAlarmsPack queryAlarms(String domain, String adapterName, String subpartitionName,
                                     String tagOfTheFirstAlarmToBeReturned, int howMany) {

        RawAlarmsPack result = new RawAlarmsPack();

        synchronized (inMemoryMap) {
            int availableAlarmsCount = inMemoryMap.size();

            Iterator<Map.Entry<String,String>> existingAlarmsIterator = inMemoryMap.entrySet().iterator();
            Map.Entry<String,String> entry = null;
            Map.Entry<String,String> theFirstAlarmToBeReturned = null;

            // Let's move to the specified "page" within our set of alarms.
            if (tagOfTheFirstAlarmToBeReturned != null) {
                while (existingAlarmsIterator.hasNext()) {
                    availableAlarmsCount--;
                    entry = existingAlarmsIterator.next();
                    if ( tagOfTheFirstAlarmToBeReturned.equals(entry.getKey()) ) {
                        // We have found the alarm to be returned as the first one.
                        break;
                    } else {
                        entry = null;
                    }
                }

                if (entry == null) {
                    // There are no more alarms to be returned.
                } else {
                    // We already have the first alarm to be returned.
                    theFirstAlarmToBeReturned = entry;
                    availableAlarmsCount++;
                }
            }

            // Let's prepare the returned object.
            int numberOfAlarmsToReturn;
            if (availableAlarmsCount > howMany) {
                numberOfAlarmsToReturn = howMany;
            } else {
                numberOfAlarmsToReturn = availableAlarmsCount;
            }
            result.alarmValues = new String[numberOfAlarmsToReturn];
            result.alarmNotificationIdentifiers = new String[numberOfAlarmsToReturn];

            // Let's load the returned alarms.
            int i = 0;

            if (theFirstAlarmToBeReturned !=null) {
                result.alarmNotificationIdentifiers[i] = theFirstAlarmToBeReturned.getKey();
                result.alarmValues[i] = theFirstAlarmToBeReturned.getValue();
                i++;
            }

            for ( ; i < numberOfAlarmsToReturn ; i++ ) {
                entry = existingAlarmsIterator.next();
                result.alarmNotificationIdentifiers[i] = entry.getKey();
                result.alarmValues[i] = entry.getValue();
            }

            // Let's calculate where to start a next page of results.
            if (existingAlarmsIterator.hasNext()) {
                entry = existingAlarmsIterator.next();
                result.tagOfTheNextAvailableAlarm = entry.getKey();
            } else {
                result.tagOfTheNextAvailableAlarm = null;
            }
        }

        return result;
    }

    @Override
    public void removeAlarm(String domain, String adapterName, String notificationIdentifier) {
        synchronized (inMemoryMap) {
            inMemoryMap.remove(notificationIdentifier);
        }
    }

    @Override
    public String[] getSubpartitions(RawAlarmsPartitionDefinition partitionDefinition) {
        return new String[] {THE_ONLY_SUBPARTITION};
    }
}

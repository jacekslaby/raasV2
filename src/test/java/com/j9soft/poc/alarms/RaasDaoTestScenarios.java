package com.j9soft.poc.alarms;

import org.junit.Assert;

import static com.j9soft.poc.alarms.RaasDaoKafkaTestConfiguration.EXISTING_ALARM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/*
 * Test scenarios that can be reused to test different DAOs.
 */
public class RaasDaoTestScenarios {

    private RaasDao dao;

    RaasDaoTestScenarios(RaasDao dao) {
        this.dao = dao;
    }

    public void t1_whenCreatedANewAlarm_thenReturnIt() {
        dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                EXISTING_ALARM.notificationIdentifier, EXISTING_ALARM.json);

        RawAlarmsPack pack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);
        
        assertThat(pack.alarmNotificationIdentifiers.length, is(1));
        assertThat(pack.alarmNotificationIdentifiers[0], is(EXISTING_ALARM.notificationIdentifier));
        assertThat(pack.alarmValues.length, is(1));
        assertThat(pack.alarmValues[0], is(EXISTING_ALARM.json));
    }

    public void t2_whenAlarmExists_thenShouldBeReturned() {

        RawAlarmsPack pack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);
        
        assertThat(pack.alarmNotificationIdentifiers.length, is(1));
        assertThat(pack.alarmNotificationIdentifiers[0], is(EXISTING_ALARM.notificationIdentifier));
        assertThat(pack.alarmValues.length, is(1));
        assertThat(pack.alarmValues[0], is(EXISTING_ALARM.json));
    }

    public void t3_whenUpsertingAnExistingAlarm_thenUpdateIt() {
        final String newJson = "{\"additionalText\":\"serious stuff\"}";

        // It updates alarm with the same NotificationIdentifier. So we expect still one result from queryAlarms.
        dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                EXISTING_ALARM.notificationIdentifier, newJson);

        RawAlarmsPack pack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);
        
        assertThat(pack.alarmNotificationIdentifiers.length, is(1));
        assertThat(pack.alarmNotificationIdentifiers[0], is(EXISTING_ALARM.notificationIdentifier));
        assertThat(pack.alarmValues.length, is(1));
        assertThat(pack.alarmValues[0], is(newJson));
    }

    public void t4_whenRemovedAnExistingAlarm_thenCreateIt() {
        final String newJson = "{\"additionalText\":\"serious stuff\"}";

        // Let's be sure that an alarm exists.
        dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                EXISTING_ALARM.notificationIdentifier, newJson);

        // Now let's remove it.
        dao.removeAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                EXISTING_ALARM.notificationIdentifier);

        // We expect that no alarms are returned.
        //
        RawAlarmsPack pack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);

        assertThat(pack.alarmNotificationIdentifiers.length, is(0));
        assertThat(pack.alarmValues.length, is(0));
    }

    public void t5_whenPutThreeAlarms_thenKeepTheirOrder() {

        // Create three alarms.
        //
        String[] notificationIdentifiers = new String[] {"eric2g:33", "siem:44", "huawei:11"};
        for (int i = 0; i < 3; i++) {
            dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                    notificationIdentifiers[i], EXISTING_ALARM.json);
        }

        // We expect that three alarms are returned. Exactly in the same order.
        //
        RawAlarmsPack pack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);

        Assert.assertArrayEquals( notificationIdentifiers, pack.alarmNotificationIdentifiers );
    }

}

package com.j9soft.poc.alarms;

import org.junit.Assert;

import static com.j9soft.poc.alarms.RaasDaoKafkaTestConfiguration.EXISTING_ALARM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/*
 * Test scenarios that can be reused to test different DAOs.
 */
public class RaasDaoTestScenarios {

    private static final String[] TEST_KEYS = new String[]{"eric2g:33", "siem:44", "huawei:11"};

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
        for (int i = 0; i < TEST_KEYS.length; i++) {
            dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                    TEST_KEYS[i], EXISTING_ALARM.json);
        }

        // We expect that three alarms are returned. Exactly in the same order.
        //
        RawAlarmsPack pack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);

        Assert.assertArrayEquals(TEST_KEYS, pack.alarmNotificationIdentifiers );
    }

    /**
     * This test must be executed on an empty topic. (i.e. a new instance of embedded broker is needed)
     */
    public void t6_whenGetTwoAlarms_thenProvideTagOfNextAlarm() {

        // Create three alarms.  (so in the log we have: 0, 1, 2)
        //
        for (int i = 0; i < TEST_KEYS.length; i++) {
            dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                    TEST_KEYS[i], EXISTING_ALARM.json);
        }
        // Let's remove all.
        //
        for (int i = 0; i < TEST_KEYS.length; i++) {
            dao.removeAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                    TEST_KEYS[i]);
        }
        // Create three alarms again.
        //
        for (int i = 0; i < TEST_KEYS.length; i++) {
            dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                    TEST_KEYS[i], EXISTING_ALARM.json);
        }
        // (so in the log we have: 0, 1, 2, x, x, x, 0, 1, 2)
        //
        // We expect:
        //  all results - three alarms are returned.
        //  first pack - two alarms are returned.
        //  second pack - three alarms are returned.
        //
        check_for_t6(TEST_KEYS,
                     new String[] {TEST_KEYS[0], TEST_KEYS[1]},
                     TEST_KEYS);

        // Update one alarm.
        // It should be returned in the first pack AND in the second pack.
        dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                TEST_KEYS[0], EXISTING_ALARM.json);
        // (so in the log we have: 0, 1, 2, x, x, x, 0, 1, 2, 0)
        //
        // We expect:
        //  all results - three alarms are returned.
        //  first pack - two alarms are returned. Exactly in the same order.
        //  second pack - three alarms are returned.
        //
        check_for_t6(new String[] {TEST_KEYS[1], TEST_KEYS[2], TEST_KEYS[0]},
                     new String[] {TEST_KEYS[0], TEST_KEYS[1]},
                     new String[] {TEST_KEYS[1], TEST_KEYS[2], TEST_KEYS[0]});

        // Update another alarm.
        dao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                TEST_KEYS[2], EXISTING_ALARM.json);
        // (so in the log we have: 0, 1, 2, x, x, x, 0, 1, 2, 0, 2)
        //
        // We expect:
        //  all results - three alarms are returned.
        //  first pack - two alarms are returned. Exactly in the same order.
        //  second pack - three alarms are returned.
        //
        check_for_t6(new String[] {TEST_KEYS[1], TEST_KEYS[0], TEST_KEYS[2]},
                     new String[] {TEST_KEYS[0], TEST_KEYS[1]},
                     new String[] {TEST_KEYS[1], TEST_KEYS[0], TEST_KEYS[2]});
    }

    private void check_for_t6(String[] allResultsExpected, String[] firstPackExpected, String[] secondPackExpected) {
        // All results - We expect that we have no tags for more alarms.
        //
        RawAlarmsPack pack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);
        Assert.assertArrayEquals(allResultsExpected, pack.alarmNotificationIdentifiers );
        assertThat(pack.tagOfTheNextAvailableAlarm, is(nullValue()));

        // First pack - We expect that we have the tag for the second pack.
        //
        RawAlarmsPack firstPack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 2);
        Assert.assertArrayEquals( firstPackExpected, firstPack.alarmNotificationIdentifiers );
        assertThat(firstPack.tagOfTheNextAvailableAlarm, notNullValue());

        // Second pack - We expect that we have no tags for more alarms.
        //
        RawAlarmsPack secondPack = dao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", firstPack.tagOfTheNextAvailableAlarm, 5);
        Assert.assertArrayEquals( secondPackExpected, secondPack.alarmNotificationIdentifiers );
        assertThat(secondPack.tagOfTheNextAvailableAlarm, is(nullValue()));
    }

}

package com.j9soft.poc.alarms;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RaasDaoDevMockTest {

    private static RaasDaoTestScenarios scenarios;

    @BeforeClass
    public static void initDao() {
        scenarios = new RaasDaoTestScenarios( new RaasDaoDevMock() );
    }

    @Test
    public void t1_whenCreatedANewAlarm_thenReturnIt() {
        scenarios.t1_whenCreatedANewAlarm_thenReturnIt();
    }

    @Test
    public void t3_whenUpsertingAnExistingAlarm_thenUpdateIt() {
        scenarios.t3_whenUpsertingAnExistingAlarm_thenUpdateIt();
    }

    @Test
    public void t4_whenRemovedAnExistingAlarm_thenCreateIt() {
        scenarios.t4_whenRemovedAnExistingAlarm_thenCreateIt();
    }

    @Test
    public void t5_whenPutThreeAlarms_thenKeepTheirOrder() {
        scenarios.t5_whenPutThreeAlarms_thenKeepTheirOrder();
    }
}

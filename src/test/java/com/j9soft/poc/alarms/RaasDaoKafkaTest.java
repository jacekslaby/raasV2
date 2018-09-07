package com.j9soft.poc.alarms;

import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RaasDaoKafkaTest {

    private static RaasDaoKafkaTestEmbeddedBroker embeddedBroker;

    private static RaasDaoKafkaTestConfiguration testConfig;
    protected RaasDao kafkaDao;
    private RaasDaoTestScenarios scenarios;

    @BeforeClass
    public static void init() throws IOException {

        // Start an embedded Kafka Server
        //
        embeddedBroker = new RaasDaoKafkaTestEmbeddedBroker();
        embeddedBroker.init();

        // Connect to the embedded Kafka.
        testConfig = new RaasDaoKafkaTestConfiguration();

        embeddedBroker.createTopic(testConfig.getTopicName());
    }

    @AfterClass
    public static void cleanup() {
        testConfig.close();
        embeddedBroker.close();
    }

    @Before
    public void initDao() {
        // Create bean to be tested.
        this.kafkaDao = testConfig.getDao();
        scenarios = new RaasDaoTestScenarios(this.kafkaDao);
    }

    @Test
    public void t1_whenCreatedANewAlarm_thenReturnIt() {
        scenarios.t1_whenCreatedANewAlarm_thenReturnIt();
    }

    @Test
    public void t2_whenAlarmExists_thenDataShouldSurviveDaoReconnect() {
        // Disconnect.
        testConfig.close();
        // Connect again to the embedded DB.  (do not create test data again !)
        testConfig = new RaasDaoKafkaTestConfiguration();
        kafkaDao = testConfig.getDao();
        scenarios = new RaasDaoTestScenarios(this.kafkaDao);

        scenarios.t2_whenAlarmExists_thenShouldBeReturned();
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

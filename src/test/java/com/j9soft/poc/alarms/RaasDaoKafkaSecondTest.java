package com.j9soft.poc.alarms;

import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RaasDaoKafkaSecondTest {

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
        kafkaDao = testConfig.getDao();
        scenarios = new RaasDaoTestScenarios(kafkaDao);
    }

    @Test
    public void t6_whenGetTwoAlarms_thenProvideTagOfNextAlarm() {
        scenarios.t6_whenGetTwoAlarms_thenProvideTagOfNextAlarm();
    }

}

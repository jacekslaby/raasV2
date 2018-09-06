package com.j9soft.poc.alarms;

import kafka.admin.RackAwareMode;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.zk.AdminZkClient;
import kafka.zk.EmbeddedZookeeper;
import kafka.zk.KafkaZkClient;
import org.apache.kafka.common.utils.Time;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static com.j9soft.poc.alarms.RaasDaoKafkaTestConfiguration.EXISTING_ALARM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RaasDaoKafkaTest {

    private static final String ZKHOST = "127.0.0.1";
    private static final String BROKERHOST = "127.0.0.1";
    private static final String BROKERPORT = "9092";

    private static EmbeddedZookeeper zkServer;
    private static KafkaZkClient zkClient;
    private static KafkaServer kafkaServer;

    private static RaasDaoKafkaTestConfiguration testConfig;
    private RaasDao kafkaDao;

    @BeforeClass
    public static void init() throws IOException {

        // Start an embedded Kafka Server
        //
        // setup Zookeeper
        zkServer = new EmbeddedZookeeper();
        String zkConnect = ZKHOST + ":" + zkServer.port();
        //
        // setup Broker
        Properties brokerProps = new Properties();
        brokerProps.setProperty("zookeeper.connect", zkConnect);
        brokerProps.setProperty("broker.id", "0");
        brokerProps.setProperty("log.dirs", Files.createTempDirectory("kafka-").toAbsolutePath().toString());
        brokerProps.setProperty("listeners", "PLAINTEXT://" + BROKERHOST +":" + BROKERPORT);
        brokerProps.setProperty("offsets.topic.replication.factor" , "1");
        KafkaConfig config = new KafkaConfig(brokerProps);
        Time mock = new MockTime();
        kafkaServer = TestUtils.createServer(config, mock);

        // Connect to the embedded Kafka.
        testConfig = new RaasDaoKafkaTestConfiguration();

        // Create topic.
        //
        Boolean isSecure = false;
        int sessionTimeoutMs = 200000;
        int connectionTimeoutMs = 15000;
        int maxInFlightRequests = 10;
        Time time = Time.SYSTEM;
        String metricGroup = "myGroup";
        String metricType = "myType";
        zkClient = KafkaZkClient.apply(zkConnect, isSecure, sessionTimeoutMs,
                connectionTimeoutMs, maxInFlightRequests, time, metricGroup, metricType);

        AdminZkClient adminZkClient = new AdminZkClient(zkClient);

        int partitions = 1;
        int replication = 1;
        Properties topicConfig = new Properties();

        adminZkClient.createTopic(testConfig.getTopicName(), partitions, replication,
                topicConfig, RackAwareMode.Disabled$.MODULE$);   // @TODO topic compaction
    }

    @AfterClass
    public static void cleanup() {
        testConfig.close();

        kafkaServer.shutdown();
        zkClient.close();

        // zkServer.shutdown();  @TODO uncomment when a patch is available for https://issues.apache.org/jira/browse/KAFKA-6291#
    }

    @Before
    public void initDao() {
        // Create bean to be tested.
        this.kafkaDao = testConfig.getDao();
    }

    @Test
    public void t1_whenCreatedANewAlarm_thenReturnIt() {
        kafkaDao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                EXISTING_ALARM.notificationIdentifier, EXISTING_ALARM.json);

        RawAlarmsPack pack = kafkaDao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);
        
        assertThat(pack.alarmNotificationIdentifiers.length, is(1));
        assertThat(pack.alarmNotificationIdentifiers[0], is(EXISTING_ALARM.notificationIdentifier));
        assertThat(pack.alarmValues.length, is(1));
        assertThat(pack.alarmValues[0], is(EXISTING_ALARM.json));
    }

    @Test
    public void t2_whenAlarmExists_thenDataShouldSurviveDaoReconnect() {
        // Disconnect.
        testConfig.close();
        // Connect again to the embedded DB.  (do not create test data again !)
        testConfig = new RaasDaoKafkaTestConfiguration();
        kafkaDao = testConfig.getDao();

        RawAlarmsPack pack = kafkaDao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);
        
        assertThat(pack.alarmNotificationIdentifiers.length, is(1));
        assertThat(pack.alarmNotificationIdentifiers[0], is(EXISTING_ALARM.notificationIdentifier));
        assertThat(pack.alarmValues.length, is(1));
        assertThat(pack.alarmValues[0], is(EXISTING_ALARM.json));
    }

    @Test
    public void t3_whenUpsertingAnExistingAlarm_thenUpdateIt() {
        final String newJson = "{\"additionalText\":\"serious stuff\"}";

        // It updates alarm with the same NotificationIdentifier. So we expect still one result from queryAlarms.
        kafkaDao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                EXISTING_ALARM.notificationIdentifier, newJson);

        RawAlarmsPack pack = kafkaDao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);
        
        assertThat(pack.alarmNotificationIdentifiers.length, is(1));
        assertThat(pack.alarmNotificationIdentifiers[0], is(EXISTING_ALARM.notificationIdentifier));
        assertThat(pack.alarmValues.length, is(1));
        assertThat(pack.alarmValues[0], is(newJson));
    }

    @Test
    public void t4_whenRemovedAnExistingAlarm_thenCreateIt() {
        final String newJson = "{\"additionalText\":\"serious stuff\"}";

        // Let's be sure that an alarm exists.
        kafkaDao.createOrUpdateAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                EXISTING_ALARM.notificationIdentifier, newJson);

        // Now let's remove it.
        kafkaDao.removeAlarm(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                EXISTING_ALARM.notificationIdentifier);

        // We expect that no alarms are returned.
        //
        RawAlarmsPack pack = kafkaDao.queryAlarms(EXISTING_ALARM.domain, EXISTING_ALARM.adapterName,
                "0", null, 5);

        assertThat(pack.alarmNotificationIdentifiers.length, is(0));
        assertThat(pack.alarmValues.length, is(0));
    }

}

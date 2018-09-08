# Raas

## Getting Started

```
git clone https://github.com/jacekslaby/raasV2.git
cd raasV2
mvn clean test
```

## Test run without connecting to Kafka broker

```
mvn exec:java            (or: mvn spring-boot:run)

GET http://localhost:8080/v2/rawalarms

PUT http://localhost:8080/v2/rawalarms/eric2g:33
{"notificationIdentifier":"eric2g:33", "perceivedSeverity"="1", "additionalText"="foo bar"}

PUT http://localhost:8080/v2/rawalarms/siem:44
{"notificationIdentifier":"siem:44", "perceivedSeverity"="2", "additionalText"="foo bar"}

PUT http://localhost:8080/v2/rawalarms/huawei:11
{"notificationIdentifier":"huawei:11", "perceivedSeverity"="1", "additionalText"="foo bar"}

GET http://localhost:8080/v2/rawalarms

GET http://localhost:8080/v2/rawalarms?howMany=2

GET http://localhost:8080/v2/rawalarms?howMany=30&tagOfTheFirstAlarmToBeReturned=2

```

## Test run with connecting to Kafka broker

1. First you need to start Kafka on 192.168.33.10:9092   (e.g. in a vagrant box)
```
Vagrantfile needs to contain:  config.vm.network "private_network", ip: "192.168.33.10"
vagrant up
vagrant ssh
cd kafka_2.11-1.1.0/config
perl -pi -e 's/#advertised.listeners.*/advertised.listeners=PLAINTEXT:\/\/192.168.33.10:9092/g'  server.properties
cd ..
bin/kafka-server-start.sh config/server.properties &
```

2. Then you may start the service and send some requests.
```
mvn spring-boot:run -Dspring-boot.run.profiles=kafka-dev -Dspring-boot.run.jvmArguments="-Dkafka-host=192.168.33.10 -Dkafka-port=9092"
(alternative: java -jar -Dspring.profiles.active=kafka-dev -Dkafka-host=192.168.33.10 -Dkafka-port=9092   target\raas-2.0-SNAPSHOT.jar)

GET http://localhost:8080/v2/rawalarms
etc.

```

3. It is possible to observe messages
```
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic tc_raw_active_alarms --from-beginning
```
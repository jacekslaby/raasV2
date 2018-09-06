# Raas

## Getting Started

```
git clone https://github.com/jacekslaby/raasV2.git
cd raasV2
mvn clean test
mvn exec:java
GET http://localhost:8080/v2/rawalarms

PUT http://localhost:8080/v2/rawalarms/eric2g:33
{"notificationIdentifier":"eric2g:33", "perceivedSeverity"="1", "additionalText"="foo bar"}

PUT http://localhost:8080/v2/rawalarms/siem:44
{"notificationIdentifier":"siem:44", "perceivedSeverity"="2", "additionalText"="foo bar"}

PUT http://localhost:8080/v2/rawalarms/huawei:11
{"notificationIdentifier":"huawei:11", "perceivedSeverity"="1", "additionalText"="foo bar"}

GET http://localhost:8080/v2/rawalarms

GET http://localhost:8080/v2/rawalarms?howMany=2

GET http://localhost:8080/v2/rawalarms?howMany=30&tagOfTheFirstAlarmToBeReturned=huawei:11

```


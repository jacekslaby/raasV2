package com.j9soft.poc.alarms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

/**
 * Servlet based implementation of Raw Active Alarms Store API (v.2).
 *
 * For each request the Domain and Adapter Name need to be provided by an implementation of javax.servlet.Filter
 * as a RequestAttribute named "partitionDefinition".
 * (See {@link AuthorizationHeaderJwtFilter} for details about the context provided in "Authorization" header.)
 *
 * This implementation is based on Spring Boot annotations for request mappings.
 * See https://spring.io/guides/gs/spring-boot/
 */
@RestController
public class RaasV2Controller implements RaasV2 {

    private static final Logger logger = LoggerFactory.getLogger(RaasV2Controller.class);

    // @Autowired  - is not used because:
    // https://spring.io/blog/2016/04/15/testing-improvements-in-spring-boot-1-4
    // "Don’t use field injection as it just makes your tests harder to write."
    //
    private final RaasDao raasDao;


    @Autowired
    RaasV2Controller(RaasDao raasDao) {

        this.raasDao = raasDao;
    }

    @Override
    @GetMapping("/v2/rawalarms")
    public RawAlarmsPack getRawAlarms(@RequestAttribute(name = "partitionDefinition") RawAlarmsPartitionDefinition partitionDefinition,
                                      @RequestParam(name = "subpartitionName", defaultValue = "0") String subpartitionName,
                                      @RequestParam(name = "tagOfTheFirstAlarmToBeReturned", required = false) String tagOfTheFirstAlarmToBeReturned,
                                      @RequestParam(name = "howMany", defaultValue = "100") int howMany) {

        logger.info("rawAlarms( domain='{}', adapterName='{}')",
                partitionDefinition.getDomain(), partitionDefinition.getAdapterName());

        return this.raasDao.queryAlarms(partitionDefinition.getDomain(), partitionDefinition.getAdapterName(),
                subpartitionName, tagOfTheFirstAlarmToBeReturned, howMany);
    }

    @Override
    @GetMapping("/v2/rawalarmssubpartitions")
    public String[] getRawAlarmsSubpartitions(RawAlarmsPartitionDefinition partitionDefinition) {
        return this.raasDao.getSubpartitions(partitionDefinition);
    }

    @Override
    @PutMapping("/v2/rawalarms/{notificationIdentifier}")
    public void putRawAlarm(@PathVariable("notificationIdentifier") String notificationIdentifier,
                            @RequestAttribute(name = "partitionDefinition") RawAlarmsPartitionDefinition partitionDefinition,
                            @RequestBody String valueObjectAsJson) {

        logger.info("putRawAlarm(notificationIdentifier='{}', domain='{}', adapterName='{}')",
                notificationIdentifier, partitionDefinition.getDomain(), partitionDefinition.getAdapterName());

        this.raasDao.createOrUpdateAlarm(partitionDefinition.getDomain(), partitionDefinition.getAdapterName(),
                notificationIdentifier, valueObjectAsJson);
    }

    @Override
    @DeleteMapping("/v2/rawalarms/{notificationIdentifier}")
    public void deleteAlarm(@PathVariable("notificationIdentifier") String notificationIdentifier,
                            @RequestAttribute(name = "partitionDefinition") RawAlarmsPartitionDefinition partitionDefinition) {

        logger.info("deleteAlarm(notificationIdentifier='{}', domain='{}', adapterName='{}')",
                notificationIdentifier, partitionDefinition.getDomain(), partitionDefinition.getAdapterName());

        this.raasDao.removeAlarm(partitionDefinition.getDomain(), partitionDefinition.getAdapterName(),
                notificationIdentifier);
    }

    @PostConstruct
    public void init() {
        Assert.notNull(raasDao, "raasDao is null!");
        logger.info("raasDao is not null - OK");
    }

}
package com.github.avpyanov.connector.testit;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"classpath:testit.properties",
        "system:env"})
public interface TestItSettings extends Config {

    String endpoint();

    String token();

    String testRunId();

    String testPlanId();

    String configurationId();
}
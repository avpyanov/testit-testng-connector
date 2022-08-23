package com.github.avpyanov.connector.testng.listeners;

import com.github.avpyanov.connector.testit.TestItSettings;
import com.github.avpyanov.testit.annotations.AutotestId;
import com.github.avpyanov.testit.client.TestItApi;
import com.github.avpyanov.testit.client.dto.TestResult;
import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import java.util.ArrayList;
import java.util.List;

public class GetTestIdsFromTestRun implements IMethodInterceptor {

    private static final Logger logger = LogManager.getLogger(GetTestIdsFromTestRun.class);

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> list, ITestContext iTestContext) {
        final List<IMethodInstance> result = new ArrayList<>();
        final List<String> testIdList = getAutotestIdsFromTestRun();
        for (IMethodInstance iMethodInstance : list) {
            String testId = getTestId(iMethodInstance);
            if (testIdList.contains(testId)) {
                result.add(iMethodInstance);
            }
        }
        return result;
    }

    private List<String> getAutotestIdsFromTestRun() {
        final List<String> testIds = new ArrayList<>();
        final TestItSettings testItSettings = ConfigFactory.create(TestItSettings.class);
        final TestItApi testItApi = new TestItApi(testItSettings.endpoint(), testItSettings.token());
        final String testRunId = testItSettings.testRunId();
        logger.info("Получение тестов для testRunId:{}", testRunId);

        List<TestResult> results = testItApi.getTestRunsClient().getTestRun(testRunId).getTestResults();
        for (TestResult result : results) {
            testIds.add(result.getAutoTest().getGlobalId());
        }
        logger.info("Тесты для запуска: {}", testIds);
        return testIds;
    }

    private String getTestId(IMethodInstance instance) {
        AutotestId autotestId = instance.getMethod()
                .getConstructorOrMethod()
                .getMethod()
                .getAnnotation(AutotestId.class);
        if (autotestId != null) {
            return autotestId.value();
        } else return "";
    }
}
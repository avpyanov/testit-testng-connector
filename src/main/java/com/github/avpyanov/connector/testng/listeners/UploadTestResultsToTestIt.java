package com.github.avpyanov.connector.testng.listeners;

import com.github.avpyanov.connector.allure.AllureResultsMapper;
import com.github.avpyanov.connector.allure.model.AllureResultsContainer;
import com.github.avpyanov.connector.testit.TestItSettings;
import com.github.avpyanov.testit.client.TestItApi;
import com.github.avpyanov.testit.client.dto.AutotestResults;
import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.TestListenerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.github.avpyanov.connector.allure.AllureUtils.*;

public class UploadTestResultsToTestIt extends TestListenerAdapter {

    private static final Logger logger = LogManager.getLogger(UploadTestResultsToTestIt.class);
    private final TestItSettings testItSettings = ConfigFactory.create(TestItSettings.class);
    private final TestItApi testItApi = new TestItApi(testItSettings.endpoint(), testItSettings.token());

    @Override
    public void onFinish(ITestContext context) {
        final String allureResultsDirectory = "target/allure-results";
        final String allureResultsPattern = "target/allure-results/%s";
        String configurationId = testItApi.getTestRunsClient().getTestRun(testItSettings.testRunId())
                .getTestResults()
                .get(0)
                .getConfigurationId();

        File[] files = new File(allureResultsDirectory).listFiles();
        if (files == null) {
            logger.error("Не удалось получить файлы из директории {}", allureResultsDirectory);
        } else {
            List<String> testResults = getAllureResults(files);
            List<AutotestResults> autotestResultsList = new ArrayList<>();
            for (String testResult : testResults) {
                AllureResultsContainer result = getResultsFromFile(allureResultsPattern, testResult);
                if (result == null) {
                    logger.error("Не удалось получить результаты для {}", testResult);
                } else {
                    final String testCaseId = getTestId(result, "autotest");

                    if (testCaseId.isEmpty()) {
                        logger.error("Не указана аннотация @AutotestId для {}", result.getFullName());
                    } else {
                        AutotestResults autotestResults = new AllureResultsMapper(testItApi, allureResultsPattern)
                                .mapToTestItResults(result);
                        autotestResults.configurationId(configurationId);
                        String externalId = testItApi.getAutotestsClient().getAutoTest(testCaseId).getExternalId();
                        autotestResults.autoTestExternalId(externalId);
                        autotestResultsList.add(autotestResults);
                    }
                }
            }
            try {
                logger.info("Загрузка результатов тест-рана {}", autotestResultsList);
                testItApi.getTestRunsClient().setAutoTestsResults(testItSettings.testRunId(), autotestResultsList);
            } catch (RuntimeException e) {
                logger.error("Не удалось загрузить результаты тест-рана: {}", e.getMessage());
            }
        }
    }
}
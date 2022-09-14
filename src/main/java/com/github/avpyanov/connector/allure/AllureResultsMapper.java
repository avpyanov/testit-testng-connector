package com.github.avpyanov.connector.allure;

import com.github.avpyanov.connector.allure.model.AllureAttachment;
import com.github.avpyanov.connector.allure.model.AllureResultsContainer;
import com.github.avpyanov.connector.allure.model.AllureResultsStep;
import com.github.avpyanov.connector.allure.model.Parameter;
import com.github.avpyanov.testit.client.TestItApi;
import com.github.avpyanov.testit.client.dto.Attachment;
import com.github.avpyanov.testit.client.dto.AutotestResults;
import com.github.avpyanov.testit.client.dto.AutotestResultsStep;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.avpyanov.connector.allure.AllureUtils.convertTimestampToDate;
import static com.github.avpyanov.connector.allure.AllureUtils.setTestStatus;

public class AllureResultsMapper {

    private static final Logger logger = LogManager.getLogger(AllureResultsMapper.class);
    private final TestItApi testItApi;
    private final String allureResultsDirectoryPattern;

    public AllureResultsMapper(TestItApi testItApi, String allureResultsDirectoryPattern) {
        this.allureResultsDirectoryPattern = allureResultsDirectoryPattern;
        this.testItApi = testItApi;
    }

    public AutotestResults mapToTestItResults(AllureResultsContainer allureResultsContainer) {
        final AutotestResults testItAutotest = new AutotestResults();

        testItAutotest.outcome(setTestStatus(allureResultsContainer.getStatus()));
        testItAutotest.startedOn(convertTimestampToDate(allureResultsContainer.getStart()));
        testItAutotest.completedOn(convertTimestampToDate(allureResultsContainer.getStop()));

        if (!allureResultsContainer.getStatus().equals("Passed")) {
            testItAutotest.message(allureResultsContainer.getStatusDetails().getMessage());
            testItAutotest.traces(allureResultsContainer.getStatusDetails().getTrace());
        }

        if (!allureResultsContainer.getAttachments().isEmpty()) {
            List<Attachment> attachments = uploadAttachments(allureResultsContainer.getAttachments(), allureResultsDirectoryPattern, testItApi);
            testItAutotest.attachments(attachments);
        }

        List<AutotestResultsStep> autotestResultsSteps = new ArrayList<>();
        List<AllureResultsStep> flattenAllureSteps = flattenSteps(allureResultsContainer.getSteps());

        for (AllureResultsStep flattenAllureStep : flattenAllureSteps) {
            AutotestResultsStep autotestResultsStep = new AutotestResultsStep();
            autotestResultsStep.setTitle(flattenAllureStep.getName());
            autotestResultsStep.setOutcome(setTestStatus(flattenAllureStep.getStatus()));
            autotestResultsStep.setStartedOn(convertTimestampToDate(flattenAllureStep.getStart()));
            autotestResultsStep.setCompletedOn(convertTimestampToDate(flattenAllureStep.getStop()));

            long duration = TimeUnit.MILLISECONDS.convert(flattenAllureStep.getStop() - flattenAllureStep.getStart(), TimeUnit.MILLISECONDS);
            autotestResultsStep.setDuration(duration);

            if (!flattenAllureStep.getParameters().isEmpty()) {
                Map<String, String> parametersMap = flattenAllureStep.getParameters()
                        .stream()
                        .collect(Collectors.toMap(Parameter::getName, Parameter::getValue));
                autotestResultsStep.setParameters(parametersMap);
            }

            if (!flattenAllureStep.getAttachments().isEmpty()) {
                List<Attachment> testItAttachments = uploadAttachments(flattenAllureStep.getAttachments(), allureResultsDirectoryPattern, testItApi);
                autotestResultsStep.setAttachments(testItAttachments);
            }

            autotestResultsSteps.add(autotestResultsStep);
        }
        testItAutotest.stepResults(autotestResultsSteps);
        return testItAutotest;
    }

    private List<AllureResultsStep> flattenSteps(final List<AllureResultsStep> steps) {
        final List<AllureResultsStep> flattenSteps = new ArrayList<>();
        for (AllureResultsStep step : steps) {
            if (step.getSteps().isEmpty()) {
                flattenSteps.add(step);
            } else {
                flattenSteps.add(step);
                flattenSteps.addAll(flattenSteps(step.getSteps()));
            }
        }
        return flattenSteps;
    }

    private List<Attachment> uploadAttachments(List<AllureAttachment> allureAttachmentList,
                                               String allureResultsDirectoryPattern,
                                               TestItApi testItApi) {
        List<Attachment> attachments = new ArrayList<>();
        for (AllureAttachment attachment : allureAttachmentList) {
            String filePath = String.format(allureResultsDirectoryPattern, attachment.getSource());
            try {
                Attachment testItAttachment = testItApi.getAttachmentsClient().createAttachment(new File(filePath));
                attachments.add(testItAttachment);
            } catch (Exception e) {
                logger.error("Не удалось загрузить вложения {} {}", filePath, e);
            }
        }
        return attachments;
    }
}
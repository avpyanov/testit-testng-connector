package com.github.avpyanov.connector.allure;

import com.github.avpyanov.testit.client.TestItApi;
import com.github.avpyanov.testit.client.dto.Attachment;
import com.github.avpyanov.testit.client.dto.AutotestResults;
import com.github.avpyanov.testit.client.dto.AutotestResultsStep;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.avpyanov.connector.allure.AllureUtils.*;

public class AllureResultsUtils {

    private static final Logger logger = LogManager.getLogger(AllureResultsUtils.class);
    private final TestItApi testItApi;
    private final String allureFolder;

    public AllureResultsUtils(TestItApi testItApi, String allureFolder) {
        this.testItApi = testItApi;
        this.allureFolder = allureFolder;
    }

    public AutotestResults getResultsFromAllure(TestResult testResult) {
        AutotestResults autotestResults = new AutotestResults();
        autotestResults.startedOn(convertTimestampToDate(testResult.getStart()))
                .completedOn(convertTimestampToDate(testResult.getStop()))
                .outcome(setTestStatus(testResult.getStatus().value()))
                .duration(getDuration(testResult.getStop(), testResult.getStart()));

        if (!testResult.getStatus().equals(Status.PASSED)) {
            autotestResults.message(testResult.getStatusDetails().getMessage())
                    .traces(testResult.getStatusDetails().getTrace());
        }

        if (!testResult.getAttachments().isEmpty()) {
            uploadAttachments(testResult.getAttachments(), allureFolder, testItApi);
        }

        List<AutotestResultsStep> autotestResultsSteps = new ArrayList<>();
        List<StepResult> stepResults = flattenSteps(testResult.getSteps());

        for (StepResult stepResult : stepResults) {
            AutotestResultsStep autotestResultsStep = new AutotestResultsStep();
            autotestResultsStep.setTitle(stepResult.getName());
            autotestResultsStep.setOutcome(setTestStatus(stepResult.getStatus().value()));
            autotestResultsStep.setStartedOn(convertTimestampToDate(stepResult.getStart()));
            autotestResultsStep.setCompletedOn(convertTimestampToDate(stepResult.getStop()));
            long duration = getDuration(stepResult.getStop(), stepResult.getStart());
            autotestResultsStep.setDuration(duration);

            if (!stepResult.getParameters().isEmpty()) {
                Map<String, String> parametersMap = stepResult.getParameters()
                        .stream()
                        .collect(Collectors.toMap(Parameter::getName, Parameter::getValue));
                autotestResultsStep.setParameters(parametersMap);
            }

            if (!stepResult.getAttachments().isEmpty()) {
                List<Attachment> testItAttachments = uploadAttachments(stepResult.getAttachments(),
                        allureFolder,
                        testItApi);
                autotestResultsStep.setAttachments(testItAttachments);
            }
            autotestResultsSteps.add(autotestResultsStep);
        }

        autotestResults.stepResults(autotestResultsSteps);
        return autotestResults;
    }

    private List<StepResult> flattenSteps(final List<StepResult> steps) {
        final List<StepResult> flattenSteps = new ArrayList<>();
        for (StepResult step : steps) {
            if (step.getSteps().isEmpty()) {
                flattenSteps.add(step);
            } else {
                flattenSteps.add(step);
                flattenSteps.addAll(flattenSteps(step.getSteps()));
            }
        }
        return flattenSteps;
    }

    private List<Attachment> uploadAttachments(List<io.qameta.allure.model.Attachment> allureAttachmentList,
                                               String allureResultsDirectoryPattern,
                                               TestItApi testItApi) {
        List<Attachment> attachments = new ArrayList<>();
        for (io.qameta.allure.model.Attachment attachment : allureAttachmentList) {
            String filePath = String.format(allureResultsDirectoryPattern, attachment.getSource());
            try {
                Attachment testItAttachment = testItApi.getAttachmentsClient().createAttachment(new File(filePath));
                attachments.add(testItAttachment);
            } catch (Exception e) {
                logger.error("Не удалось загрузить вложения {}, {}", filePath, e);
            }
        }
        return attachments;
    }
}
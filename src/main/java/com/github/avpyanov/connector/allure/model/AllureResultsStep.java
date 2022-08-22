package com.github.avpyanov.connector.allure.model;

import lombok.Data;

import java.util.List;

@Data
public class AllureResultsStep {

    private String name;
    private String status;
    private StatusDetails statusDetails;
    private List<AllureResultsStep> steps;
    private List<AllureAttachment> attachments;
    private List<Parameter> parameters;
    private Long start;
    private Long stop;
}
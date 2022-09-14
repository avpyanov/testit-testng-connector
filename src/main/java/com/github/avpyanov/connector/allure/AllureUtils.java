package com.github.avpyanov.connector.allure;

import com.github.avpyanov.connector.allure.model.AllureResultsContainer;
import com.google.gson.Gson;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.TestResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AllureUtils {

    private static final Logger logger = LogManager.getLogger(AllureUtils.class);

    public static String getTestId(TestResult result, String type) {
        Link link = result.getLinks()
                .stream()
                .filter(l -> l.getType().equals(type))
                .findFirst()
                .orElse(null);

        if (link != null) {
            return link.getName();
        } else return "";
    }

    public static String getTestId(AllureResultsContainer resultsContainer, String type) {
        com.github.avpyanov.connector.allure.model.Link autotest = resultsContainer.getLinks()
                .stream()
                .filter(link -> link.getType().equals(type))
                .findFirst()
                .orElse(null);
        if (autotest != null) {
            return autotest.getName();
        } else return "";
    }

    public static AllureResultsContainer getResultsFromFile(String allureResultsPattern, final String fileName) {
        final String filePath = String.format(allureResultsPattern, fileName);
        try {
            return new Gson().fromJson(new FileReader(filePath), AllureResultsContainer.class);
        } catch (FileNotFoundException e) {
            logger.error("Не удалось прочитать результат из  файла {}: {}", fileName, e.getMessage());
        }
        return null;
    }

    public static List<String> getAllureResults(File[] files) {
        return Stream.of(files)
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .filter(name -> name.contains("result"))
                .collect(Collectors.toList());
    }


    static String setTestStatus(String status) {
        if (status.equals("broken")) {
            return "Failed";
        } else return StringUtils.capitalize(status);
    }

    static String convertTimestampToDate(Long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(timestamp);
    }

    static long getDuration(long stopTime, long startTime) {
        return TimeUnit.MILLISECONDS.convert(stopTime - startTime, TimeUnit.MILLISECONDS);
    }

    private AllureUtils() {

    }
}

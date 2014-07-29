package com.xebialabs.xltest.fitnesse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import fitnesse.reporting.history.*;
import util.FileUtil;
import fitnesse.FitNesseContext;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.fs.FileSystemPageFactory;

import static java.lang.String.format;

public class PageHistoryExtractor {
	public static final String PAGE_NAME_TABLE_COLUMN_NAME = "id_link";
	
    private static final Logger LOG = Logger.getLogger(PageHistoryExtractor.class.getName());
    private final Feedback<TestSummary> feedback;

    public PageHistoryExtractor(Feedback<TestSummary> feedback) {
        this.feedback = feedback;
    }

    public void tellMeAboutSuite(String suiteName, String fitnesseRootLocation) throws Exception {
        File resultsDirectory = new File(fitnesseRootLocation + "FitNesseRoot/files/" + FitNesseContext.testResultsDirectoryName);

        SuiteExecutionReport suiteExecutionReport = readExecutionReport(resultsDirectory, suiteName);

        WikiPage root = new FileSystemPageFactory().makeRootPage(fitnesseRootLocation, "FitNesseRoot");
        
        for (SuiteExecutionReport.PageHistoryReference reference : suiteExecutionReport.getPageHistoryReferences()) {
        	// get tags
        	WikiPage thePage = root.getPageCrawler().getPage(PathParser.parse(reference.getPageName()));
        	Set<String> tags = makeTags(thePage);
        	Map<String, String> extraProperties = makeExtraProperties(reference.getPageName(), thePage);
        	String firstErrorMessage = findFirstErrorMessage(resultsDirectory, reference);

            feedback.found(new TestSummary(
                    reference.getPageName(),
                    tags,
                    reference.getTime(),
                    reference.getRunTimeInMillis(),
                    reference.getTestSummary().getRight(),
                    reference.getTestSummary().getWrong(),
                    reference.getTestSummary().getExceptions(),
                    firstErrorMessage,
                    extraProperties));
        }
    }

    private Set<String> makeTags(WikiPage page) {
        if (page.isRoot()) {
            return set();
        }

        String tags = page.getData().getAttribute(PageData.PropertySUITES);
        Set<String> tagSet = isNotBlank(tags) ? set(tags.split(",")) : set();
        tagSet.addAll(makeTags(page.getParent()));
        return tagSet;
    }

    private Map<String, String> makeExtraProperties(String pageName, WikiPage page) {
        if (page.isRoot()) {
            return Collections.emptyMap();
        }

        TestCaseTable table = TestCaseTable.fromPage(page);
        for (Map<String, String> row : table.asKeyValuePairs()) {
            if (row.containsKey(PAGE_NAME_TABLE_COLUMN_NAME) && pageName.equals(row.get(PAGE_NAME_TABLE_COLUMN_NAME))) {
                return row;
            }
        }
        return makeExtraProperties(pageName, page.getParent());
    }

    private String findFirstErrorMessage(File resultsDirectory, SuiteExecutionReport.PageHistoryReference reference) throws Exception {
        TestExecutionReport testExecutionReport = readExecutionReport(resultsDirectory, reference.getPageName());

        TestExecutionReport.TestResult testResult = testExecutionReport.getResults().get(0);

        for (TestExecutionReport.InstructionResult instructionResult : testResult.getInstructions()) {
            for (TestExecutionReport.Expectation expectation : instructionResult.getExpectations()) {
                if ("fail".equals(expectation.getStatus())) {
                    return format("Actual: '%s'; Expected: '%s'", expectation.getActual(), expectation.getExpected());
                } if ("error".equals(expectation.getStatus())) {
                    return expectation.getEvaluationMessage();
                }
            }
        }
        return null;
    }

    private <T extends ExecutionReport> T readExecutionReport(File resultsDirectory, String pageName) throws Exception {
        TestHistory history = new TestHistory();
        history.readPageHistoryDirectory(resultsDirectory, pageName);
        PageHistory pageHistory = history.getPageHistory(pageName);
        TestResultRecord testResultRecord = pageHistory.get(pageHistory.getLatestDate());
        return (T) ExecutionReport.makeReport(FileUtil.getFileContent(testResultRecord.getFile()));
    }

    public static Set<String> set(String... tags) {
        Set<String> tagSet = new TreeSet();
        for (String tag : tags) {
            String trimmed = tag.trim();
            if (!"".equals(trimmed)) {
                tagSet.add(trimmed);
            }
        }
        return tagSet;
    }

    private boolean isNotBlank(String tags) {
        return tags != null && !"".equals(tags);
    }

}

package org.ods.usecase

import com.cloudbees.groovy.cps.NonCPS

import groovy.json.JsonOutput

import org.ods.service.JiraZephyrService
import org.ods.util.IPipelineSteps

class JiraUseCaseZephyrSupport extends AbstractJiraUseCaseSupport {

    private JiraZephyrService zephyr

    JiraUseCaseZephyrSupport(IPipelineSteps steps, JiraUseCase usecase, JiraZephyrService zephyr) {
        super(steps, usecase)
        this.zephyr = zephyr
    }

    void applyTestResultsToAutomatedTestIssues(List jiraTestIssues, Map testResults) {
        if (!this.usecase.jira) return
        if (!this.zephyr) return

        jiraTestIssues.each { issue ->
            // Create a new execution with status UNEXECUTED
            def execution = this.zephyr.createTestExecutionForIssue(issue.id, issue.projectId).keySet().first()

            testResults.testsuites.each { testsuite ->
                testsuite.testcases.each { testcase ->
                    if (this.usecase.checkJiraIssueMatchesTestCase(issue, testcase.name)) {
                        def succeeded = !(testcase.error || testcase.failure || testcase.skipped)
                        def failed = testcase.error || testcase.failure

                        if (succeeded) {
                            this.zephyr.updateExecutionForIssuePass(execution)
                        } else if (failed) {
                            this.zephyr.updateExecutionForIssueFail(execution)
                        }
                    }
                }
            }
        }
    }

    List getAutomatedTestIssues(String projectId, String componentName = null, List<String> labelsSelector = []) {
        if (!this.usecase.jira) return []
        if (!this.zephyr) return []

        def project = this.zephyr.getProject(projectId)

        def issues = this.usecase.getIssuesForProject(projectId, componentName, ["Test"], labelsSelector << "AutomatedTest", false) { issuelink ->
            return issuelink.type.relation == "is related to" && (issuelink.issue.issuetype.name == "Epic" || issuelink.issue.issuetype.name == "Story")
        }.values().flatten()

        return issues.each { issue ->
            issue.test = [
                description: issue.description,
                details: []
            ]

            def testDetails = this.zephyr.getTestDetailsForIssue(issue.id).collect { testDetails ->
                return testDetails.subMap(["step", "data", "result"])
            }

            issue.test.details = this.sortIssuesByProperties(testDetails, ["orderId"])

            // The project ID (not key) is mandatory to generate new executions
            issue.projectId = project.id
        }
    }

    @NonCPS
    // TODO: remove
    // Sorts a collection of maps in the order of the keys in properties
    private List<Map> sortIssuesByProperties(Collection<Map> issues, List properties) {
        return issues.sort { issue ->
            issue.subMap(properties).values().join("-")
        }
    }
}

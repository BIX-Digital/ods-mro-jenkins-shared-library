package org.ods.usecase

import groovy.json.JsonOutput

import java.nio.file.Files

import org.ods.service.DocGenService
import org.ods.service.JenkinsService
import org.ods.service.LeVADocumentChaptersFileService
import org.ods.service.NexusService
import org.ods.service.OpenShiftService
import org.ods.usecase.DocGenUseCase
import org.ods.usecase.JiraUseCase
import org.ods.usecase.SonarQubeUseCase
import org.ods.util.MROPipelineUtil
import org.ods.util.PDFUtil

import spock.lang.*

import static util.FixtureHelper.*

import util.*

class LeVADocumentUseCaseSpec extends SpecHelper {

    def "compute test discrepancies"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def name = "myTests"

        when:
        def testIssues = createJiraTestIssues().each {
            it.isMissing = false
            it.isSuccess = true
        }

        def result = usecase.computeTestDiscrepancies(name, testIssues)

        then:
        result.discrepancies == "No discrepancies found."
        result.conclusion.summary == "Complete success, no discrepancies"
        result.conclusion.statement == "It is determined that all steps of the ${name} have been successfully executed and signature of this report verifies that the tests have been performed according to the plan. No discrepancies occurred."

        when:
        testIssues = createJiraTestIssues().each {
            it.isMissing = true
            it.isSuccess = false
        }

        result = usecase.computeTestDiscrepancies(name, testIssues)

        then:
        result.discrepancies == "The following minor discrepancies were found during testing: ${testIssues.collect { it.key }.join(", ")}."
        result.conclusion.summary == "Success - minor discrepancies found"
        result.conclusion.statement == "Some discrepancies were found as tests were not executed, this may be per design."

        when:
        testIssues = createJiraTestIssues().each {
            it.isMissing = false
            it.isSuccess = false
        }

        result = usecase.computeTestDiscrepancies(name, testIssues)

        then:
        result.discrepancies == "The following major discrepancies were found during testing: ${testIssues.collect { it.key }.join(", ")}."
        result.conclusion.summary == "No success - major discrepancies found"
        result.conclusion.statement == "Some discrepancies occured as tests did fail. It is not recommended to continue!"

        when:
        testIssues = createJiraTestIssues()
        testIssues[0..1].each {
            it.isMissing = true
            it.isSuccess = false
        }
        testIssues[2..4].each {
            it.isMissing = false
            it.isSuccess = false
        }

        result = usecase.computeTestDiscrepancies(name, testIssues)

        then:
        result.discrepancies == "The following major discrepancies were found during testing: ${testIssues.collect { it.key }.join(", ")}."
        result.conclusion.summary == "No success - major discrepancies found"
        result.conclusion.statement == "Some discrepancies occured as tests did fail. It is not recommended to continue!"
    }

    def "create CS"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.CS as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def uri = "http://nexus"

        when:
        usecase.createCS(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getIssuesForProject(project.id, "${documentType}:Configurable Items", ["Configuration Specification Task"], [], false, _) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Interfaces",         ["Configuration Specification Task"], [], false, _) >> [:]
        0 * jira.getIssuesForProject(project.id, *_)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create DSD"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.DSD as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def uri = "http://nexus"

        when:
        usecase.createDSD(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getIssuesForProject(project.id, null, ["System Design Specification Task"], [], false, _) >> [:]

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create DTP"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.DTP as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def testIssues = createJiraTestIssues()
        def uri = "http://nexus"

        when:
        usecase.createDTP(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getAutomatedUnitTestIssues(project.id) >> testIssues
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create DTP without Jira"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()
        project.services.jira = null

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.DTP as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def testIssues = createJiraTestIssues()
        def uri = "http://nexus"

        when:
        usecase.createDTP(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType)
        1 * levaFiles.getDocumentChapterData(documentType) >> chapterData

        then:
        1 * jira.getAutomatedUnitTestIssues(project.id) >> testIssues
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create DTR"() {
        given:
        def steps = Spy(PipelineSteps)
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(steps, util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def xmlFile = Files.createTempFile("junit", ".xml").toFile()
        xmlFile << "<?xml version='1.0' ?>\n" + createJUnitXMLTestResults()

        def project = createProject()
        def repo = project.repositories.first()
        def testReportFiles = [xmlFile]
        def testResults = new JUnitTestReportsUseCase(steps).parseTestReportFiles(testReportFiles)
        def data = [
            tests: [
                unit: [
                    testReportFiles: testReportFiles,
                    testResults: testResults
                ]
            ]
        ]

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.DTR as String
        def files = [ "raw/${xmlFile.name}": xmlFile.bytes ]

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def testIssues = createJiraTestIssues()

        when:
        usecase.createDTR(project, repo, data)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getAutomatedUnitTestIssues(project.id, "Technology-${repo.id}") >> testIssues
        1 * jira.matchJiraTestIssuesAgainstTestResults(testIssues, testResults, _, _)
        //1 * usecase.computeTestDiscrepancies("Development Tests", testIssues)
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project, repo)
        1 * usecase.createDocument(documentType, project, repo, _, files, _, null)
        _ * util.getBuildParams() >> buildParams

        cleanup:
        xmlFile.delete()
    }

    def "create DTR without Jira"() {
        given:
        def steps = Spy(PipelineSteps)
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(steps, util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def xmlFile = Files.createTempFile("junit", ".xml").toFile()
        xmlFile << "<?xml version='1.0' ?>\n" + createJUnitXMLTestResults()

        def project = createProject()
        project.services.jira = null
        def repo = project.repositories.first()
        def testReportFiles = [xmlFile]
        def testResults = new JUnitTestReportsUseCase(steps).parseTestReportFiles(testReportFiles)
        def data = [
            tests: [
                unit: [
                    testReportFiles: testReportFiles,
                    testResults: testResults
                ]
            ]
        ]

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.DTR as String
        def files = [ "raw/${xmlFile.name}": xmlFile.bytes ]

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def testIssues = createJiraTestIssues()

        when:
        usecase.createDTR(project, repo, data)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType)
        1 * levaFiles.getDocumentChapterData(documentType) >> chapterData

        then:
        1 * jira.getAutomatedUnitTestIssues(project.id, "Technology-${repo.id}") >> testIssues
        1 * jira.matchJiraTestIssuesAgainstTestResults(testIssues, testResults, _, _)
        //1 * usecase.computeTestDiscrepancies("Development Tests", testIssues)
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project, repo)
        1 * usecase.createDocument(documentType, project, repo, _, files, _, null)
        _ * util.getBuildParams() >> buildParams
    }

    def "create FTP"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.FTP as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def testIssues = createJiraTestIssues()
        def uri = "http://nexus"

        when:
        usecase.createFTP(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getAutomatedAcceptanceTestIssues(project.id) >> testIssues
        1 * jira.getAutomatedIntegrationTestIssues(project.id) >> testIssues
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create FTR"() {
        given:
        def steps = Spy(PipelineSteps)
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(steps, util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def xmlFile = Files.createTempFile("junit", ".xml").toFile()
        xmlFile << "<?xml version='1.0' ?>\n" + createJUnitXMLTestResults()

        def project = createProject()
        def repo = project.repositories.first()
        def testReportFiles = [xmlFile]
        def testResults = new JUnitTestReportsUseCase(steps).parseTestReportFiles(testReportFiles)
        def data = [
            tests: [
                acceptance: [
                    testReportFiles: testReportFiles,
                    testResults: testResults
                ],
                integration: [
                    testReportFiles: testReportFiles,
                    testResults: testResults
                ]
            ]
        ]

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.FTR as String
        def files = [ "raw/${xmlFile.name}": xmlFile.bytes ]

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def testIssues = createJiraTestIssues()
        def uri = "http://nexus"

        when:
        usecase.createFTR(project, null, data)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getAutomatedAcceptanceTestIssues(project.id) >> testIssues
        1 * jira.getAutomatedIntegrationTestIssues(project.id) >> testIssues
        2 * jira.matchJiraTestIssuesAgainstTestResults(testIssues, testResults, _, _)
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, files, null, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams

        cleanup:
        xmlFile.delete()
    }

    def "create FS"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.FS as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def uri = "http://nexus"

        when:
        usecase.createFS(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getIssuesForProject(project.id, "${documentType}:Constraints",             ["Functional Specification Task"], [], false, _) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Data",                    ["Functional Specification Task"], [], false, _) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Function",                ["Functional Specification Task"], [], false, _) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Interfaces",              ["Functional Specification Task"], [], false, _) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Operational Environment", ["Functional Specification Task"], [], false, _) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Roles",                   ["Functional Specification Task"], [], false, _) >> [:]
        0 * jira.getIssuesForProject(project.id, *_)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create IVP"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.IVP as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def testIssues = createJiraTestIssues()
        def uri = "http://nexus"

        when:
        usecase.createIVP(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getAutomatedInstallationTestIssues(project.id) >> testIssues
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create IVR"() {
        given:
        def steps = Spy(PipelineSteps)
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(steps, util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def xmlFile = Files.createTempFile("junit", ".xml").toFile()
        xmlFile << "<?xml version='1.0' ?>\n" + createJUnitXMLTestResults()

        def project = createProject()
        def repo = project.repositories.first()
        def testReportFiles = [xmlFile]
        def testResults = new JUnitTestReportsUseCase(steps).parseTestReportFiles(testReportFiles)
        def data = [
            tests: [
                installation: [
                    testReportFiles: testReportFiles,
                    testResults: testResults
                ]
            ]
        ]

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.IVR as String
        def files = [ "raw/${xmlFile.name}": xmlFile.bytes ]

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def testIssues = createJiraTestIssues()
        def uri = "http://nexus"

        when:
        usecase.createIVR(project, null, data)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getAutomatedInstallationTestIssues(project.id) >> testIssues
        1 * jira.matchJiraTestIssuesAgainstTestResults(testIssues, testResults, _, _)
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, files, null, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams

        cleanup:
        xmlFile.delete()
    }

    def "create SCP"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.SCP as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def uri = "http://nexus"

        when:
        usecase.createSCP(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create SCP without Jira"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()
        project.services.jira = null

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.SCP as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def uri = "http://nexus"

        when:
        usecase.createSCP(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType)
        1 * levaFiles.getDocumentChapterData(documentType) >> chapterData

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create SCR"() {
        given:
        def buildParams = createBuildEnvironment(env)

        def steps = Spy(PipelineSteps)
        steps.env.BUILD_ID = "0815"

        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(steps, util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()
        def repo = project.repositories.first()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.SCR as String
        def sqReportsPath = "sonarqube/${repo.id}"
        def sqReportsStashName = "scrr-report-${repo.id}-${steps.env.BUILD_ID}"
        def files = [ "${usecase.getDocumentBasename("SCRR", buildParams.version, steps.env.BUILD_ID, project, repo)}.docx": getResource("Test.docx").bytes ]

        // Stubbed Method Responses
        def chapterData = ["sec1": "myContent"]
        def sqReportFiles = [ getResource("Test.docx") ]

        when:
        usecase.createSCR(project, repo)

        then:
        1 * jenkins.unstashFilesIntoPath(sqReportsStashName, "${steps.env.WORKSPACE}/${sqReportsPath}", "SonarQube Report") >> true
        1 * sq.loadReportsFromPath("${steps.env.WORKSPACE}/${sqReportsPath}") >> sqReportFiles

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project, repo)
        1 * usecase.createDocument(documentType, project, repo, _, files, _, null)
        _ * util.getBuildParams() >> buildParams
    }

    def "create SCR without Jira"() {
        given:
        def buildParams = createBuildEnvironment(env)

        def steps = Spy(PipelineSteps)
        steps.env.BUILD_ID = "0815"

        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(steps, util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()
        project.services.jira = null
        def repo = project.repositories.first()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.SCR as String
        def sqReportsPath = "sonarqube/${repo.id}"
        def sqReportsStashName = "scrr-report-${repo.id}-${steps.env.BUILD_ID}"
        def files = [ "${usecase.getDocumentBasename("SCRR", buildParams.version, steps.env.BUILD_ID, project, repo)}.docx": getResource("Test.docx").bytes ]

        // Stubbed Method Responses
        def chapterData = ["sec1": "myContent"]
        def sqReportFiles = [ getResource("Test.docx") ]

        when:
        usecase.createSCR(project, repo)

        then:
        1 * jenkins.unstashFilesIntoPath(sqReportsStashName, "${steps.env.WORKSPACE}/${sqReportsPath}", "SonarQube Report") >> true
        1 * sq.loadReportsFromPath("${steps.env.WORKSPACE}/${sqReportsPath}") >> sqReportFiles

        then:
        1 * jira.getDocumentChapterData(project.id, documentType)
        1 * levaFiles.getDocumentChapterData(documentType) >> chapterData

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project, repo)
        1 * usecase.createDocument(documentType, project, repo, _, files, _, null)
        _ * util.getBuildParams() >> buildParams
    }

    def "create SDS"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()
        def repo = project.repositories.first()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.SDS as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]

        when:
        usecase.createSDS(project, repo)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project, repo)
        1 * usecase.createDocument(documentType, project, repo, _, [:], _, null)
        _ * util.getBuildParams() >> buildParams
    }

    def "create TIP"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.TIP as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def uri = "http://nexus"

        when:
        usecase.createTIP(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create TIP without Jira"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()
        project.services.jira = null

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.TIP as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def uri = "http://nexus"

        when:
        usecase.createTIP(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType)
        1 * levaFiles.getDocumentChapterData(documentType) >> chapterData

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create TIR"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()
        def repo = project.repositories.first()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.TIR as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]

        when:
        usecase.createTIR(project, repo)

        then:
        1 * os.getPodDataForComponent(repo.id) >> createOpenShiftPodDataForComponent()

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project, repo)
        1 * usecase.createDocument(documentType, project, repo, _, [:], _, null)
        _ * util.getBuildParams() >> buildParams
    }

    def "create TIR without Jira"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()
        project.services.jira = null
        def repo = project.repositories.first()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.TIR as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]

        when:
        usecase.createTIR(project, repo)

        then:
        1 * os.getPodDataForComponent(repo.id) >> createOpenShiftPodDataForComponent()

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project, repo)
        1 * usecase.createDocument(documentType, project, repo, _, [:], _, null)
        _ * util.getBuildParams() >> buildParams
    }

    def "create URS"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.URS as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def chapterData = ["sec1": "myContent"]
        def uri = "http://nexus"

        when:
        usecase.createURS(project)

        then:
        1 * jira.getDocumentChapterData(project.id, documentType) >> chapterData
        0 * levaFiles.getDocumentChapterData(documentType)

        then:
        1 * jira.getIssuesForProject(project.id, "${documentType}:Availability",            ["Epic"]) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Compatibility",           ["Epic"]) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Interfaces",              ["Epic"]) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Operational",             ["Epic"]) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Operational Environment", ["Epic"]) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Performance",             ["Epic"]) >> [:]
        1 * jira.getIssuesForProject(project.id, "${documentType}:Procedural Constraints",  ["Epic"]) >> [:]
        0 * jira.getIssuesForProject(project.id, *_)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType], project)
        1 * usecase.createDocument(documentType, project, null, _, [:], _, null) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create overall DTR"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.DTR as String
        def documentTypeName = LeVADocumentUseCase.DocumentType.OVERALL_DTR as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def uri = "http://nexus"

        when:
        usecase.createOverallDTR(project)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentTypeName], project)
        1 * usecase.createOverallDocument("Overall-Cover", documentType, _, project) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentTypeName]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create overall SCR"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.SCR as String
        def documentTypeName = LeVADocumentUseCase.DocumentType.OVERALL_SCR as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def uri = "http://nexus"

        when:
        usecase.createOverallSCR(project)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentTypeName], project)
        1 * usecase.createOverallDocument("Overall-Cover", documentType, _, project) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentTypeName]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create overall SDS"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.SDS as String
        def documentTypeName = LeVADocumentUseCase.DocumentType.OVERALL_SDS as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def uri = "http://nexus"

        when:
        usecase.createOverallSDS(project)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentTypeName], project)
        1 * usecase.createOverallDocument("Overall-Cover", documentType, _, project) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentTypeName]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "create overall TIR"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        // Test Parameters
        def project = createProject()

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.TIR as String
        def documentTypeName = LeVADocumentUseCase.DocumentType.OVERALL_TIR as String

        // Stubbed Method Responses
        def buildParams = createBuildEnvironment(env)
        def uri = "http://nexus"

        when:
        usecase.createOverallTIR(project)

        then:
        1 * usecase.getDocumentMetadata(LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentTypeName], project)
        1 * usecase.createOverallDocument("Overall-TIR-Cover", documentType, _, project, _) >> uri
        1 * jira.notifyLeVaDocumentTrackingIssue(project.id, documentType, "A new ${LeVADocumentUseCase.DOCUMENT_TYPE_NAMES[documentTypeName]} has been generated and is available at: ${uri}.")
        _ * util.getBuildParams() >> buildParams
    }

    def "get supported documents"() {
        given:
        def util = Mock(MROPipelineUtil)
        def docGen = Mock(DocGenService)
        def jenkins = Mock(JenkinsService)
        def jira = Mock(JiraUseCase)
        def levaFiles = Mock(LeVADocumentChaptersFileService)
        def nexus = Mock(NexusService)
        def os = Mock(OpenShiftService)
        def pdf = Mock(PDFUtil)
        def sq = Mock(SonarQubeUseCase)
        def usecase = Spy(new LeVADocumentUseCase(Spy(PipelineSteps), util, docGen, jenkins, jira, levaFiles, nexus, os, pdf, sq))

        when:
        def result = usecase.getSupportedDocuments()

        then:
        result.size() == 20

        then:
        result.contains("CS")
        result.contains("DSD")
        result.contains("DTP")
        result.contains("DTR")
        result.contains("FS")
        result.contains("FTP")
        result.contains("FTR")
        result.contains("IVP")
        result.contains("IVR")
        result.contains("SCP")
        result.contains("SCR")
        result.contains("SDS")
        result.contains("TIP")
        result.contains("TIR")
        result.contains("URS")
        result.contains("OVERALL_DTR")
        result.contains("OVERALL_IVR")
        result.contains("OVERALL_SCR")
        result.contains("OVERALL_SDS")
        result.contains("OVERALL_TIR")
    }
}

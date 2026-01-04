package com.testing.automation.Mapper;

import com.testing.automation.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UiTestMapper {
    // UI Test Case
    void insertCase(UiTestCase uiTestCase);

    void updateCase(UiTestCase uiTestCase);

    UiTestCase findCaseById(Long id);

    List<UiTestCase> findCasesByModuleId(Long moduleId);

    List<UiTestCase> findCasesByProjectId(Long projectId);

    List<UiTestCase> findAllCases();

    void deleteCaseById(Long id);

    // UI Test Step
    void insertStep(UiTestStep uiTestStep);

    void updateStep(UiTestStep uiTestStep);

    List<UiTestStep> findStepsByCaseId(Long caseId);

    void deleteStepsByCaseId(Long caseId);

    void deleteStepById(Long id);

    // UI Test Execution Record
    void insertRecord(UiTestExecutionRecord record);

    void updateRecord(UiTestExecutionRecord record);

    UiTestExecutionRecord findRecordById(Long id);

    List<UiTestExecutionRecord> findRecordsByCaseId(Long caseId);

    List<UiTestExecutionRecord> findRecordsByProjectId(Long projectId);

    List<UiTestExecutionRecord> findAllRecords();

    // UI Test Execution Log
    void insertLog(UiTestExecutionLog log);

    void updateLog(UiTestExecutionLog log);

    List<UiTestExecutionLog> findLogsByRecordId(Long recordId);
}

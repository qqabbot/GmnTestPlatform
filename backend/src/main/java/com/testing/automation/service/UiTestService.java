package com.testing.automation.service;

import com.testing.automation.mapper.UiTestMapper;
import com.testing.automation.model.UiTestCase;
import com.testing.automation.model.UiTestStep;
import com.testing.automation.model.UiTestExecutionRecord;
import com.testing.automation.model.UiTestExecutionLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UiTestService {
    private final UiTestMapper uiTestMapper;

    @Transactional
    public UiTestCase saveCase(UiTestCase uiTestCase) {
        log.info("SERVICE: Saving UI Case ID={}, Name={}, Headless={}, Headers={}, AutoDismiss={}",
                uiTestCase.getId(), uiTestCase.getName(), uiTestCase.getHeadless(),
                uiTestCase.getCustomHeaders(), uiTestCase.getAutoDismissDialogs());
        if (uiTestCase.getId() == null) {
            uiTestMapper.insertCase(uiTestCase);
        } else {
            uiTestMapper.updateCase(uiTestCase);
        }
        // Smart Sync Logic: Delete missing, Update existing, Insert new, Resolve Temp
        // IDs

        // 1. Identify IDs to delete
        List<UiTestStep> existingSteps = uiTestMapper.findStepsByCaseId(uiTestCase.getId());
        java.util.Set<Long> incomingIds = uiTestCase.getSteps().stream()
                .map(UiTestStep::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        for (UiTestStep existing : existingSteps) {
            if (!incomingIds.contains(existing.getId())) {
                uiTestMapper.deleteStepById(existing.getId());
            }
        }

        // 2. Map for Temp IDs (TempString -> NewDbId)
        java.util.Map<String, Long> tempIdMap = new java.util.HashMap<>();

        // 3. Upsert Steps (Pass 1: Data persistence)
        for (UiTestStep step : uiTestCase.getSteps()) {
            step.setCaseId(uiTestCase.getId());

            // If parent refers to a temp ID, we temporarily set parentId to null (to
            // satisfy FK if strictly validated, or just logical order)
            // We will fix relationships in Pass 2
            if (step.getParentTempId() != null) {
                step.setParentId(null);
            }

            if (step.getId() != null) {
                uiTestMapper.updateStep(step);
            } else {
                uiTestMapper.insertStep(step);
                if (step.getTempId() != null) {
                    tempIdMap.put(step.getTempId(), step.getId());
                }
            }
        }

        // 4. Fix Relationships (Pass 2: Temp Parent Resolution)
        for (UiTestStep step : uiTestCase.getSteps()) {
            if (step.getParentTempId() != null) {
                Long newParentId = tempIdMap.get(step.getParentTempId());
                if (newParentId != null) {
                    step.setParentId(newParentId);
                    uiTestMapper.updateStep(step);
                }
            }
        }
        return uiTestCase;
    }

    public UiTestCase getCaseById(Long id) {
        return uiTestMapper.findCaseById(id);
    }

    public List<UiTestCase> getCasesByModule(Long moduleId) {
        return uiTestMapper.findCasesByModuleId(moduleId);
    }

    public List<UiTestCase> getCasesByProject(Long projectId) {
        return uiTestMapper.findCasesByProjectId(projectId);
    }

    public List<UiTestCase> getAllCases() {
        return uiTestMapper.findAllCases();
    }

    @Transactional
    public void deleteCase(Long id) {
        uiTestMapper.deleteStepsByCaseId(id);
        uiTestMapper.deleteCaseById(id);
    }

    @Transactional
    public void saveStep(UiTestStep step) {
        if (step.getId() == null) {
            uiTestMapper.insertStep(step);
        } else {
            uiTestMapper.updateStep(step);
        }
    }

    public List<UiTestStep> getStepsByCase(Long caseId) {
        return uiTestMapper.findStepsByCaseId(caseId);
    }

    @Transactional
    public void deleteStep(Long id) {
        uiTestMapper.deleteStepById(id);
    }

    // Execution Records & Logs
    public List<UiTestExecutionRecord> getRecordsByCase(Long caseId) {
        return uiTestMapper.findRecordsByCaseId(caseId);
    }

    public List<UiTestExecutionRecord> getRecordsByProject(Long projectId) {
        return uiTestMapper.findRecordsByProjectId(projectId);
    }

    public List<UiTestExecutionRecord> getAllRecords() {
        return uiTestMapper.findAllRecords();
    }

    public UiTestExecutionRecord getRecordById(Long id) {
        return uiTestMapper.findRecordById(id);
    }

    public List<UiTestExecutionLog> getLogsByRecord(Long recordId) {
        return uiTestMapper.findLogsByRecordId(recordId);
    }
}

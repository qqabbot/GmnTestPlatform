package com.testing.automation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.automation.Mapper.TestScenarioMapper;
import com.testing.automation.Mapper.TestScenarioStepMapper;
import com.testing.automation.dto.TestScenarioStepDTO;
import com.testing.automation.model.TestScenario;
import com.testing.automation.model.TestScenarioStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestScenarioService {

    @Autowired
    private TestScenarioMapper scenarioMapper;

    @Autowired
    private TestScenarioStepMapper stepMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TestScenario> findAll() {
        return scenarioMapper.findAll();
    }

    public List<TestScenario> findByProjectId(Long projectId) {
        return scenarioMapper.findByProjectId(projectId);
    }

    public TestScenario findById(Long id) {
        return scenarioMapper.findById(id);
    }

    @Transactional
    public TestScenario create(TestScenario scenario) {
        if (scenario.getProjectId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        scenario.setCreatedAt(LocalDateTime.now());
        scenario.setUpdatedAt(LocalDateTime.now());
        scenarioMapper.insert(scenario);
        return scenario;
    }

    @Transactional
    public TestScenario update(Long id, TestScenario scenario) {
        TestScenario existing = scenarioMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Scenario not found");
        }
        existing.setName(scenario.getName());
        existing.setDescription(scenario.getDescription());
        existing.setProjectId(scenario.getProjectId());
        existing.setUpdatedAt(LocalDateTime.now());
        scenarioMapper.update(existing);
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        scenarioMapper.deleteById(id);
    }

    // --- Step Management ---

    /**
     * Get the full tree of steps for a scenario
     */
    public List<TestScenarioStepDTO> getScenarioStepsTree(Long scenarioId) {
        List<TestScenarioStep> steps = stepMapper.findByScenarioId(scenarioId);
        return buildStepTree(steps);
    }

    @Transactional
    public void addStep(TestScenarioStep step) {
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        // Simple append logic: find max order index for this parent and add 1
        // For strict ordering, frontend should provide index or we implement reordering
        // logic
        stepMapper.insert(step);
    }

    @Transactional
    public void updateStep(Long id, TestScenarioStep updatedStep) {
        TestScenarioStep existing = stepMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Step not found");
        }
        // Update fields
        existing.setName(updatedStep.getName());
        existing.setType(updatedStep.getType());
        existing.setControlLogic(updatedStep.getControlLogic());
        existing.setDataOverrides(updatedStep.getDataOverrides());
        existing.setReferenceCaseId(updatedStep.getReferenceCaseId());
        existing.setParentId(updatedStep.getParentId()); // Potential move
        existing.setOrderIndex(updatedStep.getOrderIndex());

        existing.setUpdatedAt(LocalDateTime.now());
        stepMapper.update(existing);
    }

    @Transactional
    public void deleteStep(Long id) {
        // Cascade delete is handled by DB FK usually, but if logical delete needed:
        // Or if we need to clean up children recursively manually (if DB cascade not
        // set)
        // Assuming DB Cascade ON DELETE CASCADE
        stepMapper.deleteById(id);
    }

    // --- Tree Sync ---

    @Transactional
    public void syncSteps(Long scenarioId, List<TestScenarioStepDTO> rootSteps) {
        if (scenarioId == null) {
            throw new IllegalArgumentException("Scenario ID cannot be null");
        }
        
        log.info("Syncing steps for scenario {}", scenarioId);
        
        // 1. Delete all existing steps
        // Note: DELETE CASCADE on self-referencing table should handle children
        // but we use deleteByScenarioId which should be safe
        stepMapper.deleteByScenarioId(scenarioId);
        log.debug("Deleted existing steps for scenario {}", scenarioId);

        // 2. Recursive Insert
        if (rootSteps != null && !rootSteps.isEmpty()) {
            for (int i = 0; i < rootSteps.size(); i++) {
                try {
                    saveStepRecursively(scenarioId, null, rootSteps.get(i), i);
                } catch (Exception e) {
                    log.error("Failed to save step at index {}: {}", i, e.getMessage(), e);
                    throw new RuntimeException("Failed to sync steps: " + e.getMessage(), e);
                }
            }
            log.info("Successfully synced {} root steps for scenario {}", rootSteps.size(), scenarioId);
        } else {
            log.info("No steps to sync for scenario {}", scenarioId);
        }
    }

    private void saveStepRecursively(Long scenarioId, Long parentId, TestScenarioStepDTO dto, int order) {
        TestScenarioStep step = new TestScenarioStep();
        step.setScenarioId(scenarioId);
        step.setParentId(parentId);
        step.setType(dto.getType());
        step.setName(dto.getName());
        step.setReferenceCaseId(dto.getReferenceCaseId());
        step.setOrderIndex(order);
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());

        // Serialize JSON fields
        try {
            if (dto.getControlLogic() != null) {
                step.setControlLogic(objectMapper.writeValueAsString(dto.getControlLogic()));
            }
            if (dto.getDataOverrides() != null) {
                step.setDataOverrides(objectMapper.writeValueAsString(dto.getDataOverrides()));
            }
        } catch (Exception e) {
            log.error("Failed to serialize JSON fields for step {}: {}", dto.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to serialize step data: " + e.getMessage(), e);
        }

        // Insert to get ID
        stepMapper.insert(step);

        // Process Children
        if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
            for (int i = 0; i < dto.getChildren().size(); i++) {
                saveStepRecursively(scenarioId, step.getId(), dto.getChildren().get(i), i);
            }
        }
    }

    // Helper to build tree from flat list
    private List<TestScenarioStepDTO> buildStepTree(List<TestScenarioStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, TestScenarioStepDTO> dtoMap = new HashMap<>();
        List<TestScenarioStepDTO> roots = new ArrayList<>();

        // 1. Convert all to DTO
        for (TestScenarioStep step : steps) {
            try {
                TestScenarioStepDTO dto = convertToDTO(step);
                dtoMap.put(step.getId(), dto);
            } catch (Exception e) {
                log.error("Failed to convert step {} to DTO: {}", step.getId(), e.getMessage(), e);
                // Continue with other steps
            }
        }

        // 2. Build hierarchy
        for (TestScenarioStep step : steps) {
            TestScenarioStepDTO dto = dtoMap.get(step.getId());
            if (dto == null) {
                continue; // Skip if conversion failed
            }
            
            if (step.getParentId() == null) {
                roots.add(dto);
            } else {
                TestScenarioStepDTO parent = dtoMap.get(step.getParentId());
                if (parent != null) {
                    parent.getChildren().add(dto);
                } else {
                    // Orphaned node (parent deleted?), treat as root for safety
                    log.warn("Step {} has parent {} which doesn't exist, treating as root", 
                            step.getId(), step.getParentId());
                    roots.add(dto);
                }
            }
        }

        // 3. Sort by orderIndex (nulls last)
        Comparator<TestScenarioStepDTO> orderComparator = Comparator
                .comparing(TestScenarioStepDTO::getOrderIndex, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
        
        roots.sort(orderComparator);
        
        // Sort children recursively
        for (TestScenarioStepDTO root : roots) {
            sortChildrenRecursively(root, orderComparator);
        }

        return roots;
    }

    /**
     * Recursively sort children of a step node
     */
    private void sortChildrenRecursively(TestScenarioStepDTO node, Comparator<TestScenarioStepDTO> comparator) {
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            node.getChildren().sort(comparator);
            for (TestScenarioStepDTO child : node.getChildren()) {
                sortChildrenRecursively(child, comparator);
            }
        }
    }

    private TestScenarioStepDTO convertToDTO(TestScenarioStep step) {
        TestScenarioStepDTO dto = new TestScenarioStepDTO();
        BeanUtils.copyProperties(step, dto);

        // Parse JSON strings to Objects
        try {
            if (step.getControlLogic() != null && !step.getControlLogic().trim().isEmpty()) {
                dto.setControlLogic(objectMapper.readTree(step.getControlLogic()));
            }
            if (step.getDataOverrides() != null && !step.getDataOverrides().trim().isEmpty()) {
                dto.setDataOverrides(objectMapper.readTree(step.getDataOverrides()));
            }
        } catch (Exception e) {
            log.warn("Failed to parse step JSON for step {}: {}", step.getId(), e.getMessage());
            // Set to null to indicate parsing failure, but don't fail the entire conversion
            dto.setControlLogic(null);
            dto.setDataOverrides(null);
        }

        return dto;
    }
}

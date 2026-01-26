package com.testing.automation.service.invoker;

import com.testing.automation.dto.RunnerContext;
import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestResult;
import com.testing.automation.dto.TestScenarioStepDTO;
import com.testing.automation.service.ScenarioExecutionEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Invoker for nested scenario steps
 * Executes a referenced scenario within the current scenario execution
 */
@Slf4j
@Component
public class ScenarioStepInvoker implements StepInvoker {
    
    @Autowired
    @Lazy
    private ScenarioExecutionEngine executionEngine;
    
    @Override
    public String getSupportedType() {
        return "SCENARIO";
    }
    
    @Override
    public void execute(TestScenarioStepDTO step,
                       RunnerContext context,
                       List<TestResult> results,
                       Consumer<ScenarioExecutionEvent> eventListener) {
        if (step.getReferenceScenarioId() == null) {
            log.warn("Scenario step {} has no reference scenario ID, skipping", step.getId());
            return;
        }
        
        // Check for circular reference
        if (isCircularReference(step.getReferenceScenarioId(), context)) {
            log.error("Circular reference detected for scenario {}", step.getReferenceScenarioId());
            TestResult errorResult = TestResult.builder()
                    .caseId(step.getId())
                    .caseName(step.getName())
                    .status("FAIL")
                    .message("Circular reference detected: scenario references itself directly or indirectly")
                    .build();
            results.add(errorResult);
            context.recordStepResult(step.getId(), errorResult);
            return;
        }
        
        try {
            String stepName = step.getName() != null ? step.getName() : "Nested Scenario";
            log.info(">>> Start Executing Nested Scenario Step: {} (scenario ID: {})", 
                    stepName, step.getReferenceScenarioId());
            
            // Register step for step reference syntax
            context.registerStep(step.getId(), stepName);
            
            // Push new scope for nested scenario
            context.pushScope();
            
            try {
                // Execute the nested scenario using the same context
                // Pass the context to maintain variable scope and circular reference tracking
                // Note: envKey should be stored in context or passed through step configuration
                // For now, we'll extract it from context variables or use a default
                String envKey = context.getVariable("_envKey") != null 
                    ? context.getVariable("_envKey").toString() 
                    : "dev";
                
                List<TestResult> nestedResults = executionEngine.executeScenario(
                    step.getReferenceScenarioId(), 
                    envKey,
                    eventListener,
                    context // Pass context for nested execution
                );
                
                // Add nested results to main results
                results.addAll(nestedResults);
                
                // Merge nested scenario variables to parent scope
                // Preserve all new variables created in nested scenario
                context.mergeToParentScope(null);
                
                log.info("<<< Finished Nested Scenario Step. Executed {} steps", nestedResults.size());
                
            } finally {
                context.popScope();
            }
            
        } catch (Exception e) {
            log.error("Exception executing nested scenario step: {}", e.getMessage(), e);
            TestResult errorResult = TestResult.builder()
                    .caseId(step.getId())
                    .caseName(step.getName())
                    .status("FAIL")
                    .message("Execution Error: " + e.getMessage())
                    .build();
            results.add(errorResult);
            context.recordStepResult(step.getId(), errorResult);
        }
    }
    
    /**
     * Check for circular reference
     * Uses the executing scenarios set in RunnerContext
     */
    private boolean isCircularReference(Long scenarioId, RunnerContext context) {
        return context.isExecutingScenario(scenarioId);
    }
}

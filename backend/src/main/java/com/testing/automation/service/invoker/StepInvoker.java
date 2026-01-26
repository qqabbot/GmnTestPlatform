package com.testing.automation.service.invoker;

import com.testing.automation.dto.RunnerContext;
import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestResult;
import com.testing.automation.dto.TestScenarioStepDTO;

import java.util.List;
import java.util.function.Consumer;

/**
 * Step Invoker Interface
 * Strategy pattern for executing different types of scenario steps
 */
public interface StepInvoker {
    /**
     * Execute a scenario step
     * @param step step to execute
     * @param context runner context with variable scopes
     * @param results list to collect execution results
     * @param eventListener event listener for real-time updates
     */
    void execute(TestScenarioStepDTO step,
                 RunnerContext context,
                 List<TestResult> results,
                 Consumer<ScenarioExecutionEvent> eventListener);
    
    /**
     * Get the step type this invoker handles
     * @return step type (e.g., "CASE", "LOOP", "IF")
     */
    String getSupportedType();
}

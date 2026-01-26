package com.testing.automation.service.invoker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.automation.dto.RunnerContext;
import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestResult;
import com.testing.automation.dto.TestScenarioStepDTO;
import com.testing.automation.service.TestCaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class ScriptStepInvoker implements StepInvoker {
    
    @Autowired
    private TestCaseService testCaseService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getSupportedType() {
        return "SCRIPT";
    }
    
    @Override
    public void execute(TestScenarioStepDTO step,
                       RunnerContext context,
                       List<TestResult> results,
                       Consumer<ScenarioExecutionEvent> eventListener) {
        try {
            if (step.getControlLogic() == null) {
                log.warn("Script step {} has no control logic", step.getId());
                return;
            }
            
            JsonNode logic = objectMapper.valueToTree(step.getControlLogic());
            String script = logic.has("script") ? logic.get("script").asText() : "";
            
            if (script == null || script.trim().isEmpty()) {
                log.warn("Script step {} has empty script", step.getId());
                return;
            }
            
            log.debug("Executing script step: {}", step.getName());
            
            // Register step for step reference syntax
            context.registerStep(step.getId(), step.getName());
            
            // Execute script (may set variables in context)
            Object result = testCaseService.executeScript(script, context.getAllVariables());
            
            // Create a synthetic test result for script execution
            TestResult scriptResult = TestResult.builder()
                    .caseId(step.getId())
                    .caseName(step.getName() != null ? step.getName() : "Script Step")
                    .status("PASS")
                    .message("Script executed successfully")
                    .build();
            results.add(scriptResult);
            context.recordStepResult(step.getId(), scriptResult);
            
            // Notify script complete
            if (eventListener != null) {
                eventListener.accept(ScenarioExecutionEvent.builder()
                        .type("step_complete")
                        .stepId(step.getId())
                        .stepName(step.getName())
                        .status("PASS")
                        .variables(new HashMap<>(context.getAllVariables()))
                        .timestamp(System.currentTimeMillis())
                        .build());
            }
        } catch (Exception e) {
            log.error("Error executing script step: {}", e.getMessage(), e);
            TestResult scriptResult = TestResult.builder()
                    .caseId(step.getId())
                    .caseName(step.getName() != null ? step.getName() : "Script Step")
                    .status("FAIL")
                    .message("Script execution error: " + e.getMessage())
                    .build();
            results.add(scriptResult);
            context.recordStepResult(step.getId(), scriptResult);
        }
    }
}

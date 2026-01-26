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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class IfStepInvoker implements StepInvoker {
    
    @Autowired
    @Lazy
    private StepInvokerRegistry invokerRegistry;
    
    @Autowired
    private TestCaseService testCaseService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getSupportedType() {
        return "IF";
    }
    
    @Override
    public void execute(TestScenarioStepDTO step,
                       RunnerContext context,
                       List<TestResult> results,
                       Consumer<ScenarioExecutionEvent> eventListener) {
        boolean conditionMet = false;
        try {
            if (step.getControlLogic() != null) {
                JsonNode logic = objectMapper.valueToTree(step.getControlLogic());
                if (logic.has("condition")) {
                    String condition = logic.get("condition").asText();
                    if (condition != null && !condition.trim().isEmpty()) {
                        Object result = testCaseService.executeScript(condition, context.getAllVariables());
                        if (result instanceof Boolean) {
                            conditionMet = (Boolean) result;
                        } else if (result != null) {
                            conditionMet = Boolean.parseBoolean(result.toString());
                        }
                    } else {
                        conditionMet = true; // Empty condition = ALWAYS
                    }
                } else {
                    conditionMet = true;
                }
            } else {
                conditionMet = true;
            }
        } catch (Exception e) {
            log.error("Error evaluating IF condition: {}", e.getMessage(), e);
            conditionMet = false; // Fail safe
        }
        
        if (conditionMet) {
            // Push new scope for if block
            context.pushScope();
            try {
                executeChildren(step, context, results, eventListener);
            } finally {
                // Merge if block variables to parent scope
                context.mergeToParentScope(null);
                context.popScope();
            }
        } else {
            log.debug("IF condition not met, skipping children");
        }
    }
    
    private void executeChildren(TestScenarioStepDTO step, RunnerContext context,
                                List<TestResult> results, Consumer<ScenarioExecutionEvent> eventListener) {
        if (step.getChildren() != null && !step.getChildren().isEmpty()) {
            for (TestScenarioStepDTO child : step.getChildren()) {
                StepInvoker invoker = invokerRegistry.getInvoker(child.getType());
                if (invoker != null) {
                    invoker.execute(child, context, results, eventListener);
                } else {
                    log.warn("No invoker found for step type: {}", child.getType());
                }
            }
        }
    }
}

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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Component
public class LoopStepInvoker implements StepInvoker {
    
    @Autowired
    @Lazy
    private StepInvokerRegistry invokerRegistry;
    
    @Autowired
    private TestCaseService testCaseService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getSupportedType() {
        return "LOOP";
    }
    
    @Override
    public void execute(TestScenarioStepDTO step,
                       RunnerContext context,
                       List<TestResult> results,
                       Consumer<ScenarioExecutionEvent> eventListener) {
        try {
            if (step.getControlLogic() == null) {
                // Default to 1 count loop if no logic defined
                executeChildren(step, context, results, eventListener);
                return;
            }
            
            JsonNode logic = objectMapper.valueToTree(step.getControlLogic());
            String mode = logic.has("mode") ? logic.get("mode").asText() : "count";
            
            if ("while".equalsIgnoreCase(mode)) {
                executeWhileLoop(step, logic, context, results, eventListener);
            } else if ("foreach".equalsIgnoreCase(mode)) {
                executeForEachLoop(step, logic, context, results, eventListener);
            } else {
                // Default: Count Loop
                executeCountLoop(step, logic, context, results, eventListener);
            }
        } catch (Exception e) {
            log.error("Error in executeLoopStep: {}", e.getMessage(), e);
        }
    }
    
    private void executeCountLoop(TestScenarioStepDTO step, JsonNode logic,
                                  RunnerContext context, List<TestResult> results,
                                  Consumer<ScenarioExecutionEvent> eventListener) {
        int count = logic.has("count") ? logic.get("count").asInt() : 1;
        log.debug("Executing count loop: {} iterations", count);
        
        for (int i = 0; i < count; i++) {
            // Push new scope for loop iteration
            context.pushScope();
            context.setVariable("loopIndex", i);
            context.setVariable("loopIteration", i + 1);
            
            try {
                executeChildren(step, context, results, eventListener);
            } finally {
                // Merge loop variables to parent scope (preserve new variables)
                Set<String> preserveKeys = null; // Preserve all new variables
                context.mergeToParentScope(preserveKeys);
                context.popScope();
            }
        }
    }
    
    private void executeForEachLoop(TestScenarioStepDTO step, JsonNode logic,
                                    RunnerContext context, List<TestResult> results,
                                    Consumer<ScenarioExecutionEvent> eventListener) {
        String iterableVar = logic.has("iterableVar") ? logic.get("iterableVar").asText() : "";
        String itemVar = logic.has("itemVar") ? logic.get("itemVar").asText() : "item";
        String indexVar = logic.has("indexVar") ? logic.get("indexVar").asText() : "index";
        
        Object iterableObj = context.getVariable(iterableVar);
        if (iterableObj instanceof Collection) {
            Collection<?> collection = (Collection<?>) iterableObj;
            int i = 0;
            for (Object item : collection) {
                // Push new scope for each iteration
                context.pushScope();
                context.setVariable(itemVar, item);
                context.setVariable(indexVar, i);
                
                try {
                    executeChildren(step, context, results, eventListener);
                } finally {
                    // Merge iteration variables to parent scope
                    Set<String> preserveKeys = null; // Preserve all new variables except loop vars
                    context.mergeToParentScope(preserveKeys);
                    context.popScope();
                }
                i++;
            }
        } else {
            log.error("ForEach target '{}' is not a collection: {}", iterableVar, iterableObj);
        }
    }
    
    private void executeWhileLoop(TestScenarioStepDTO step, JsonNode logic,
                                 RunnerContext context, List<TestResult> results,
                                 Consumer<ScenarioExecutionEvent> eventListener) {
        String condition = logic.has("condition") ? logic.get("condition").asText() : "true";
        int maxIterations = logic.has("maxIterations") ? logic.get("maxIterations").asInt() : 100;
        
        log.debug("Executing while loop: condition={}, maxIterations={}", condition, maxIterations);
        
        int iteration = 0;
        while (iteration < maxIterations) {
            // Evaluate condition
            boolean conditionMet = evaluateCondition(condition, context);
            if (!conditionMet) {
                log.debug("While loop condition not met, exiting at iteration {}", iteration);
                break;
            }
            
            // Push new scope for iteration
            context.pushScope();
            context.setVariable("loopIndex", iteration);
            context.setVariable("loopIteration", iteration + 1);
            
            try {
                executeChildren(step, context, results, eventListener);
            } finally {
                // Merge iteration variables to parent scope
                Set<String> preserveKeys = null;
                context.mergeToParentScope(preserveKeys);
                context.popScope();
            }
            
            iteration++;
        }
        
        if (iteration >= maxIterations) {
            log.warn("While loop reached max iterations ({})", maxIterations);
        }
    }
    
    private boolean evaluateCondition(String condition, RunnerContext context) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }
        
        try {
            // Use TestCaseService to evaluate Groovy expression
            Object result = testCaseService.executeScript(condition, context.getAllVariables());
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result != null) {
                return Boolean.parseBoolean(result.toString());
            }
            return false;
        } catch (Exception e) {
            log.error("Error evaluating while condition: {}", e.getMessage(), e);
            return false;
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

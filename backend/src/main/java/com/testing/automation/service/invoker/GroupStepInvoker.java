package com.testing.automation.service.invoker;

import com.testing.automation.dto.RunnerContext;
import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestResult;
import com.testing.automation.dto.TestScenarioStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class GroupStepInvoker implements StepInvoker {
    
    @Autowired
    @Lazy
    private StepInvokerRegistry invokerRegistry;
    
    @Override
    public String getSupportedType() {
        return "GROUP";
    }
    
    @Override
    public void execute(TestScenarioStepDTO step,
                       RunnerContext context,
                       List<TestResult> results,
                       Consumer<ScenarioExecutionEvent> eventListener) {
        // Group is just a container, execute all children sequentially
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

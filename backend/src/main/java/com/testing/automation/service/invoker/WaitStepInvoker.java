package com.testing.automation.service.invoker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.automation.dto.RunnerContext;
import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestResult;
import com.testing.automation.dto.TestScenarioStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class WaitStepInvoker implements StepInvoker {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getSupportedType() {
        return "WAIT";
    }
    
    @Override
    public void execute(TestScenarioStepDTO step,
                       RunnerContext context,
                       List<TestResult> results,
                       Consumer<ScenarioExecutionEvent> eventListener) {
        long waitMs = 1000;
        try {
            if (step.getControlLogic() != null) {
                JsonNode logic = objectMapper.valueToTree(step.getControlLogic());
                if (logic.has("waitMs")) {
                    waitMs = logic.get("waitMs").asLong();
                }
            }
            log.debug("Waiting {} ms", waitMs);
            Thread.sleep(waitMs);
            
            // Notify wait complete
            if (eventListener != null) {
                eventListener.accept(ScenarioExecutionEvent.builder()
                        .type("step_complete")
                        .stepId(step.getId())
                        .stepName(step.getName())
                        .status("PASS")
                        .timestamp(System.currentTimeMillis())
                        .build());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wait step interrupted");
        } catch (Exception e) {
            log.error("Error in wait step: {}", e.getMessage(), e);
        }
    }
}

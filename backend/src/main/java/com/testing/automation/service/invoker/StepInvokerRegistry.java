package com.testing.automation.service.invoker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for Step Invokers
 * Automatically discovers and registers all StepInvoker implementations
 */
@Slf4j
@Component
public class StepInvokerRegistry {
    
    @Autowired
    private List<StepInvoker> invokers;
    
    private Map<String, StepInvoker> invokerMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        for (StepInvoker invoker : invokers) {
            String type = invoker.getSupportedType();
            if (type != null) {
                invokerMap.put(type.toUpperCase(), invoker);
                log.debug("Registered StepInvoker for type: {}", type);
            }
        }
        log.info("Initialized StepInvokerRegistry with {} invokers", invokerMap.size());
    }
    
    /**
     * Get invoker for step type
     * @param stepType step type (e.g., "CASE", "LOOP", "IF")
     * @return StepInvoker or null if not found
     */
    public StepInvoker getInvoker(String stepType) {
        if (stepType == null) {
            return null;
        }
        return invokerMap.get(stepType.toUpperCase());
    }
    
    /**
     * Check if invoker exists for step type
     * @param stepType step type
     * @return true if invoker exists
     */
    public boolean hasInvoker(String stepType) {
        return stepType != null && invokerMap.containsKey(stepType.toUpperCase());
    }
}

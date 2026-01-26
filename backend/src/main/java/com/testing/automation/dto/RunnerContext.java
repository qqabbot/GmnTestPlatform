package com.testing.automation.dto;

import lombok.Data;
import java.util.*;

/**
 * Runner Context with scope stack support
 * Supports local scopes (e.g., variables within loops) and variable isolation
 */
@Data
public class RunnerContext {
    /**
     * Scope stack: each level represents a scope (global, loop, condition block, etc.)
     * The top of the stack is the current scope
     */
    private Stack<Map<String, Object>> scopes = new Stack<>();
    
    /**
     * Step execution history for reference
     */
    private Map<Long, TestResult> executionHistory = new HashMap<>();
    
    /**
     * Step name to step ID mapping for step reference syntax
     */
    private Map<String, Long> stepNameToIdMap = new HashMap<>();
    
    /**
     * Step ID to step name mapping
     */
    private Map<Long, String> stepIdToNameMap = new HashMap<>();
    
    /**
     * Set of scenario IDs currently being executed (for circular reference detection)
     */
    private Set<Long> executingScenarios = new HashSet<>();
    
    public RunnerContext() {
        // Initialize with global scope
        pushScope();
    }
    
    /**
     * Push a new scope onto the stack
     * Used when entering a loop, condition block, or nested scenario
     */
    public void pushScope() {
        scopes.push(new HashMap<>());
    }
    
    /**
     * Pop the current scope from the stack
     * Used when exiting a loop, condition block, or nested scenario
     * Variables in the popped scope are discarded (unless explicitly merged)
     */
    public void popScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        } else {
            throw new IllegalStateException("Cannot pop the global scope");
        }
    }
    
    /**
     * Get variable value, searching from current scope to global scope
     * @param key variable name
     * @return variable value, or null if not found
     */
    public Object getVariable(String key) {
        // Search from top (current scope) to bottom (global scope)
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Object> scope = scopes.get(i);
            if (scope.containsKey(key)) {
                return scope.get(key);
            }
        }
        return null;
    }
    
    /**
     * Set variable in the current scope
     * @param key variable name
     * @param value variable value
     */
    public void setVariable(String key, Object value) {
        if (scopes.isEmpty()) {
            pushScope();
        }
        scopes.peek().put(key, value);
    }
    
    /**
     * Set variable in the global scope (persists across all scopes)
     * @param key variable name
     * @param value variable value
     */
    public void setGlobalVariable(String key, Object value) {
        if (scopes.isEmpty()) {
            pushScope();
        }
        scopes.get(0).put(key, value);
    }
    
    /**
     * Check if variable exists in any scope
     * @param key variable name
     * @return true if variable exists
     */
    public boolean hasVariable(String key) {
        return getVariable(key) != null;
    }
    
    /**
     * Merge current scope variables into parent scope
     * Used when exiting a loop to preserve variables created inside
     * @param preserveKeys keys to preserve (if null, preserve all new keys)
     */
    public void mergeToParentScope(Set<String> preserveKeys) {
        if (scopes.size() <= 1) {
            return; // No parent scope
        }
        
        Map<String, Object> currentScope = scopes.peek();
        Map<String, Object> parentScope = scopes.get(scopes.size() - 2);
        
        for (Map.Entry<String, Object> entry : currentScope.entrySet()) {
            String key = entry.getKey();
            // Only merge if key is in preserveKeys (or preserveKeys is null for all)
            if (preserveKeys == null || preserveKeys.contains(key)) {
                // Only set if parent doesn't already have this key (don't overwrite)
                if (!parentScope.containsKey(key)) {
                    parentScope.put(key, entry.getValue());
                }
            }
        }
    }
    
    /**
     * Get all variables in current scope
     * @return map of variables in current scope
     */
    public Map<String, Object> getCurrentScopeVariables() {
        if (scopes.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(scopes.peek());
    }
    
    /**
     * Get all variables across all scopes (flattened)
     * Current scope variables take precedence
     * @return map of all variables
     */
    public Map<String, Object> getAllVariables() {
        Map<String, Object> allVars = new HashMap<>();
        // Start from global scope (bottom) to current scope (top)
        for (Map<String, Object> scope : scopes) {
            allVars.putAll(scope);
        }
        return allVars;
    }
    
    /**
     * Register step for step reference syntax
     * @param stepId step ID
     * @param stepName step name
     */
    public void registerStep(Long stepId, String stepName) {
        if (stepId != null && stepName != null) {
            stepNameToIdMap.put(stepName, stepId);
            stepIdToNameMap.put(stepId, stepName);
        }
    }
    
    /**
     * Get step ID by step name
     * @param stepName step name
     * @return step ID or null
     */
    public Long getStepIdByName(String stepName) {
        return stepNameToIdMap.get(stepName);
    }
    
    /**
     * Get step name by step ID
     * @param stepId step ID
     * @return step name or null
     */
    public String getStepNameById(Long stepId) {
        return stepIdToNameMap.get(stepId);
    }
    
    /**
     * Get execution result for a step
     * @param stepId step ID
     * @return test result or null
     */
    public TestResult getStepResult(Long stepId) {
        return executionHistory.get(stepId);
    }
    
    /**
     * Record execution result for a step
     * @param stepId step ID
     * @param result test result
     */
    public void recordStepResult(Long stepId, TestResult result) {
        executionHistory.put(stepId, result);
    }
    
    /**
     * Get current scope depth
     * @return depth (1 = global scope only)
     */
    public int getScopeDepth() {
        return scopes.size();
    }
    
    /**
     * Add scenario to executing set (for circular reference detection)
     * @param scenarioId scenario ID
     */
    public void addExecutingScenario(Long scenarioId) {
        executingScenarios.add(scenarioId);
    }
    
    /**
     * Remove scenario from executing set
     * @param scenarioId scenario ID
     */
    public void removeExecutingScenario(Long scenarioId) {
        executingScenarios.remove(scenarioId);
    }
    
    /**
     * Check if scenario is currently being executed
     * @param scenarioId scenario ID
     * @return true if scenario is executing
     */
    public boolean isExecutingScenario(Long scenarioId) {
        return executingScenarios.contains(scenarioId);
    }
    
    /**
     * Get copy of executing scenarios set
     * @return set of executing scenario IDs
     */
    public Set<Long> getExecutingScenarios() {
        return new HashSet<>(executingScenarios);
    }
}

package com.testing.automation.service;

import com.testing.automation.mapper.GlobalVariableMapper;
import com.testing.automation.model.GlobalVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GlobalVariableService {

    @Autowired
    private GlobalVariableMapper variableMapper;

    public List<GlobalVariable> getAllVariables() {
        return variableMapper.findAll();
    }

    public GlobalVariable createVariable(GlobalVariable variable) {
        variable.setCreatedAt(LocalDateTime.now());
        variable.setUpdatedAt(LocalDateTime.now());
        variableMapper.insert(variable);
        return variable;
    }

    public GlobalVariable getVariableById(Long id) {
        return variableMapper.findById(id);
    }

    public GlobalVariable getVariableByKey(String key) {
        return variableMapper.findByVarKey(key);
    }

    public GlobalVariable updateVariable(Long id, GlobalVariable varDetails) {
        GlobalVariable var = getVariableById(id);
        if (var != null) {
            var.setKeyName(varDetails.getKeyName());
            var.setValueContent(varDetails.getValueContent());
            var.setType(varDetails.getType());
            var.setDescription(varDetails.getDescription());
            var.setUpdatedAt(LocalDateTime.now());
            variableMapper.update(var);
        }
        return var;
    }

    public void deleteVariable(Long id) {
        variableMapper.deleteById(id);
    }

    public List<GlobalVariable> getVariablesByEnvironmentId(Long envId) {
        return variableMapper.findByEnvironmentId(envId);
    }

    // Additional methods for test execution
    public Map<String, Object> getVariablesMapByEnvName(String envName) {
        List<GlobalVariable> variables = variableMapper.findAll();
        Map<String, Object> map = new HashMap<>();
        for (GlobalVariable var : variables) {
            if (var.getEnvironment() != null && envName.equals(var.getEnvironment().getEnvName())) {
                map.put(var.getKeyName(), var.getValueContent());
            }
        }
        return map;
    }

    public Map<String, Object> getVariablesMapWithInheritance(Long projectId, Long moduleId, String envKey) {
        List<GlobalVariable> variables = variableMapper.findAll();
        Map<String, Object> map = new HashMap<>();

        // Order: Global -> Project -> Module -> Environment
        for (GlobalVariable var : variables) {
            boolean include = false;

            if (var.getProjectId() == null && var.getModuleId() == null && var.getEnvironmentId() == null) {
                include = true; // Global variable
            } else if (projectId != null && var.getProjectId() != null && var.getProjectId().equals(projectId)) {
                include = true; // Project-level
            } else if (moduleId != null && var.getModuleId() != null && var.getModuleId().equals(moduleId)) {
                include = true; // Module-level
            } else if (envKey != null && var.getEnvironment() != null
                    && envKey.equals(var.getEnvironment().getEnvName())) {
                include = true; // Environment-level
            }

            if (include) {
                map.put(var.getKeyName(), var.getValueContent());
            }
        }
        return map;
    }
}

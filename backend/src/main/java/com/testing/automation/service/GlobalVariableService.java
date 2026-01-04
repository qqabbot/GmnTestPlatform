package com.testing.automation.service;

import com.testing.automation.Mapper.GlobalVariableMapper;
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
        // Extract IDs from nested objects if present
        if (variable.getEnvironment() != null && variable.getEnvironment().getId() != null) {
            variable.setEnvironmentId(variable.getEnvironment().getId());
        }
        if (variable.getProject() != null && variable.getProject().getId() != null) {
            variable.setProjectId(variable.getProject().getId());
        }
        if (variable.getModule() != null && variable.getModule().getId() != null) {
            variable.setModuleId(variable.getModule().getId());
        }
        
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
            
            // Extract IDs from nested objects if present
            if (varDetails.getEnvironment() != null && varDetails.getEnvironment().getId() != null) {
                var.setEnvironmentId(varDetails.getEnvironment().getId());
            }
            if (varDetails.getProject() != null && varDetails.getProject().getId() != null) {
                var.setProjectId(varDetails.getProject().getId());
            }
            if (varDetails.getModule() != null && varDetails.getModule().getId() != null) {
                var.setModuleId(varDetails.getModule().getId());
            }
            
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

        // Try to parse envKey as ID
        Long envId = null;
        try {
            if (envKey != null) {
                envId = Long.parseLong(envKey);
            }
        } catch (NumberFormatException e) {
            // envKey is likely a name, ignore
        }

        // Order: Global -> Project -> Module -> Environment
        // Process in priority order to ensure correct override behavior
        
        // 1. Global variables (no project, module, or environment)
        for (GlobalVariable var : variables) {
            if (var.getProjectId() == null && var.getModuleId() == null && var.getEnvironmentId() == null) {
                map.put(var.getKeyName(), var.getValueContent());
            }
        }
        
        // 2. Project variables (override Global)
        if (projectId != null) {
            for (GlobalVariable var : variables) {
                if (var.getProjectId() != null && var.getProjectId().equals(projectId) 
                    && var.getModuleId() == null && var.getEnvironmentId() == null) {
                    map.put(var.getKeyName(), var.getValueContent());
                }
            }
        }
        
        // 3. Module variables (override Project)
        if (moduleId != null) {
            for (GlobalVariable var : variables) {
                if (var.getModuleId() != null && var.getModuleId().equals(moduleId) 
                    && var.getEnvironmentId() == null) {
                    map.put(var.getKeyName(), var.getValueContent());
                }
            }
        }
        
        // 4. Environment variables (override all)
        for (GlobalVariable var : variables) {
            if (var.getEnvironmentId() != null) {
                boolean match = false;
                if (envId != null && envId.equals(var.getEnvironmentId())) {
                    match = true;
                } else if (envKey != null && var.getEnvironment() != null
                        && envKey.equalsIgnoreCase(var.getEnvironment().getEnvName())) {
                    match = true;
                }
                if (match) {
                    map.put(var.getKeyName(), var.getValueContent());
                }
            }
        }
        
        return map;
    }
}

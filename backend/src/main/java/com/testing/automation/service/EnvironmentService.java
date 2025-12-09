package com.testing.automation.service;

import com.testing.automation.mapper.EnvironmentMapper;
import com.testing.automation.model.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnvironmentService {

    @Autowired
    private EnvironmentMapper environmentMapper;

    public List<Environment> getAllEnvironments() {
        return environmentMapper.findAll();
    }

    public Environment createEnvironment(Environment environment) {
        environment.setCreatedAt(LocalDateTime.now());
        environment.setUpdatedAt(LocalDateTime.now());
        environmentMapper.insert(environment);
        return environment;
    }

    public Environment getEnvironmentById(Long id) {
        return environmentMapper.findById(id);
    }

    public Environment getEnvironmentByName(String name) {
        return environmentMapper.findByEnvName(name);
    }

    public Environment updateEnvironment(Long id, Environment envDetails) {
        Environment env = getEnvironmentById(id);
        if (env != null) {
            env.setEnvName(envDetails.getEnvName());
            env.setDescription(envDetails.getDescription());
            env.setDomain(envDetails.getDomain());
            env.setUpdatedAt(LocalDateTime.now());
            environmentMapper.update(env);
        }
        return env;
    }

    public void deleteEnvironment(Long id) {
        environmentMapper.deleteById(id);
    }
}

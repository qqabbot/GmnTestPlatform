package com.testing.automation.service;

import com.testing.automation.Mapper.TestModuleMapper;
import com.testing.automation.model.TestModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestModuleService {

    @Autowired
    private TestModuleMapper moduleMapper;

    public List<TestModule> getAllModules() {
        return moduleMapper.findAll();
    }

    public List<TestModule> getModulesByProjectId(Long projectId) {
        return moduleMapper.findByProjectId(projectId);
    }

    public TestModule createModule(TestModule module) {
        module.setCreatedAt(LocalDateTime.now());
        module.setUpdatedAt(LocalDateTime.now());
        moduleMapper.insert(module);
        return module;
    }

    public TestModule getModuleById(Long id) {
        return moduleMapper.findById(id);
    }

    public TestModule updateModule(Long id, TestModule moduleDetails) {
        TestModule module = getModuleById(id);
        if (module != null) {
            module.setModuleName(moduleDetails.getModuleName());
            module.setUpdatedAt(LocalDateTime.now());
            moduleMapper.update(module);
        }
        return module;
    }

    public void deleteModule(Long id) {
        moduleMapper.deleteById(id);
    }
}

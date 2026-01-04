package com.testing.automation.service;

import com.testing.automation.Mapper.TestStepTemplateMapper;
import com.testing.automation.model.TestStepTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestStepTemplateService {

    @Autowired
    private TestStepTemplateMapper templateMapper;

    public List<TestStepTemplate> getAllTemplates() {
        return templateMapper.findAll();
    }

    public List<TestStepTemplate> findAll(Long projectId) {
        if (projectId != null) {
            return templateMapper.findByProjectIdOrGlobal(projectId);
        }
        return templateMapper.findAll();
    }

    public TestStepTemplate create(TestStepTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);
        return template;
    }

    public TestStepTemplate getTemplateById(Long id) {
        return templateMapper.findById(id);
    }

    public TestStepTemplate update(Long id, TestStepTemplate templateDetails) {
        TestStepTemplate template = getTemplateById(id);
        if (template != null) {
            template.setName(templateDetails.getName());
            template.setMethod(templateDetails.getMethod());
            template.setUrl(templateDetails.getUrl());
            template.setHeaders(templateDetails.getHeaders());
            template.setBody(templateDetails.getBody());
            template.setAssertionScript(templateDetails.getAssertionScript());
            template.setUpdatedAt(LocalDateTime.now());
            templateMapper.update(template);
        }
        return template;
    }

    public void delete(Long id) {
        templateMapper.deleteById(id);
    }

    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }
}

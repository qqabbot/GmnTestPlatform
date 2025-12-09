package com.testing.automation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.automation.mapper.ProjectMapper;
import com.testing.automation.mapper.TestCaseMapper;
import com.testing.automation.mapper.TestModuleMapper;
import com.testing.automation.mapper.TestStepMapper;
import com.testing.automation.model.Project;
import com.testing.automation.model.TestCase;
import com.testing.automation.model.TestModule;
import com.testing.automation.model.TestStep;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImportService {

    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private TestModuleMapper moduleMapper;
    @Autowired
    private TestCaseMapper caseMapper;
    @Autowired
    private TestStepMapper stepMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public int importSwagger(Long projectId, String contentOrUrl, boolean isUrl) {
        OpenAPI openAPI;
        if (isUrl) {
            openAPI = new OpenAPIV3Parser().read(contentOrUrl);
        } else {
            openAPI = new OpenAPIV3Parser().readContents(contentOrUrl).getOpenAPI();
        }

        if (openAPI == null) {
            throw new RuntimeException("Failed to parse Swagger/OpenAPI content");
        }

        Project project = projectMapper.findById(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found");
        }

        int count = 0;
        Map<String, TestModule> moduleCache = new HashMap<>();

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem item = entry.getValue();

            if (item.getGet() != null) {
                createCaseFromOp(project, path, "GET", item.getGet(), moduleCache);
                count++;
            }
            if (item.getPost() != null) {
                createCaseFromOp(project, path, "POST", item.getPost(), moduleCache);
                count++;
            }
            if (item.getPut() != null) {
                createCaseFromOp(project, path, "PUT", item.getPut(), moduleCache);
                count++;
            }
            if (item.getDelete() != null) {
                createCaseFromOp(project, path, "DELETE", item.getDelete(), moduleCache);
                count++;
            }
        }
        return count;
    }

    private void createCaseFromOp(Project project, String path, String method, Operation op,
            Map<String, TestModule> moduleCache) {
        String tagName = (op.getTags() != null && !op.getTags().isEmpty()) ? op.getTags().get(0) : "Default";

        TestModule module = moduleCache.computeIfAbsent(tagName, k -> {
            // Check if module exists
            for (TestModule m : moduleMapper.findByProjectId(project.getId())) {
                if (m.getModuleName().equals(k)) {
                    return m;
                }
            }
            // Create new module
            TestModule m = new TestModule();
            m.setModuleName(k);
            m.setProjectId(project.getId());
            m.setCreatedAt(LocalDateTime.now());
            m.setUpdatedAt(LocalDateTime.now());
            moduleMapper.insert(m);
            return m;
        });

        TestCase testCase = new TestCase();
        testCase.setCaseName(op.getSummary() != null ? op.getSummary() : method + " " + path);
        testCase.setModuleId(module.getId());
        testCase.setMethod(method);
        testCase.setUrl(path);
        testCase.setHeaders("{}");
        testCase.setBody("{}");
        testCase.setAssertionScript("assert status_code == 200");
        testCase.setIsActive(true);
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());
        caseMapper.insert(testCase);

        TestStep step = new TestStep();
        step.setCaseId(testCase.getId());
        step.setStepName("Request");
        step.setStepOrder(1);
        step.setMethod(method);
        step.setUrl(path);
        step.setHeaders("{}");
        step.setBody("{}");
        step.setAssertionScript("assert status_code == 200");
        step.setEnabled(true);
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        stepMapper.insert(step);
    }
}

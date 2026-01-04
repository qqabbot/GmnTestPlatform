package com.testing.automation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.automation.Mapper.ProjectMapper;
import com.testing.automation.Mapper.TestCaseMapper;
import com.testing.automation.Mapper.TestModuleMapper;
import com.testing.automation.Mapper.TestStepMapper;
import com.testing.automation.model.Project;
import com.testing.automation.model.TestCase;
import com.testing.automation.model.TestModule;
import com.testing.automation.model.TestStep;
import com.testing.automation.util.CurlParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public int importSwagger(Long projectId, String contentOrUrl, boolean isUrl, boolean clearData) {
        if (clearData) {
            clearProjectData(projectId);
        }

        OpenAPI openAPI;
        io.swagger.parser.OpenAPIParser parser = new io.swagger.parser.OpenAPIParser();
        io.swagger.v3.parser.core.models.SwaggerParseResult result;

        if (isUrl) {
            result = parser.readLocation(contentOrUrl, null, null);
        } else {
            result = parser.readContents(contentOrUrl, null, null);
        }
        openAPI = result.getOpenAPI();

        if (openAPI == null) {
            throw new RuntimeException("Failed to parse Swagger/OpenAPI content. Errors: " + result.getMessages());
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

    private void clearProjectData(Long projectId) {
        List<TestModule> modules = moduleMapper.findByProjectId(projectId);
        for (TestModule module : modules) {
            List<TestCase> cases = caseMapper.findByModuleId(module.getId());
            for (TestCase testCase : cases) {
                stepMapper.deleteByCaseId(testCase.getId());
                caseMapper.deleteById(testCase.getId());
            }
            moduleMapper.deleteById(module.getId());
        }
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

        // Generate Body
        String body = "{}";
        if (op.getRequestBody() != null && op.getRequestBody().getContent() != null
                && op.getRequestBody().getContent().get("application/json") != null) {
            Schema<?> schema = op.getRequestBody().getContent().get("application/json").getSchema();
            Map<String, Object> bodyMap = parseSchema(schema);
            try {
                body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bodyMap);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // Handle path variables (e.g. /users/{id} -> /users/${id})
        String url = path;
        Pattern p = Pattern.compile("\\{([^}]+)\\}");
        Matcher m = p.matcher(url);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "\\${" + m.group(1) + "}");
        }
        m.appendTail(sb);
        url = "${base_url}" + sb.toString();

        TestCase testCase = new TestCase();
        testCase.setCaseName(op.getSummary() != null ? op.getSummary() : method + " " + path);
        testCase.setModuleId(module.getId());
        testCase.setMethod(method);
        testCase.setUrl(url);
        testCase.setHeaders("{}");
        testCase.setBody(body);
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
        step.setUrl(url);
        step.setHeaders("{}");
        step.setBody(body);
        step.setAssertionScript("assert status_code == 200");
        step.setEnabled(true);
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        stepMapper.insert(step);
    }

    private Map<String, Object> parseSchema(Schema<?> schema) {
        Map<String, Object> result = new HashMap<>();
        if (schema == null)
            return result;

        // Handle $ref (simplified) - loop prevention needed in real world, but basic
        // for now
        // OpenAPIV3Parser usually resolves refs if configured, but let's assume
        // flattened or we handle properties

        if (schema.getProperties() != null) {
            for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                String key = entry.getKey();
                Schema propSchema = entry.getValue();

                Object value;
                if ("integer".equals(propSchema.getType()) || "number".equals(propSchema.getType())) {
                    if (key.toLowerCase().endsWith("id")) {
                        value = "${" + key + "}"; // Dynamic association
                    } else {
                        value = 0;
                    }
                } else if ("string".equals(propSchema.getType())) {
                    if (key.toLowerCase().endsWith("id")) {
                        value = "${" + key + "}";
                    } else {
                        value = "string";
                    }
                } else if ("boolean".equals(propSchema.getType())) {
                    value = true;
                } else if ("array".equals(propSchema.getType())) {
                    // Simple array support
                    value = List.of();
                } else if ("object".equals(propSchema.getType())) {
                    value = parseSchema(propSchema);
                } else {
                    value = null;
                }
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Parse cURL command and return parsed data (without creating database records)
     * 
     * @param curlCommand cURL command string
     * @return CurlParseResult with method, url, headers, and body
     */
    public CurlParser.CurlParseResult parseCurl(String curlCommand) {
        return CurlParser.parse(curlCommand);
    }
    
    /**
     * Import test case or step from cURL command
     * 
     * @param projectId Project ID
     * @param moduleId Module ID (optional, will create default module if not provided)
     * @param curlCommand cURL command string
     * @param asStep Whether to create as a step (true) or as a test case (false)
     * @param caseId If asStep is true, the test case ID to add the step to
     * @return Created TestCase or TestStep
     */
    @Transactional
    public Object importFromCurl(Long projectId, Long moduleId, String curlCommand, boolean asStep, Long caseId) {
        try {
            CurlParser.CurlParseResult parsed = CurlParser.parse(curlCommand);
            
            if (asStep && caseId != null) {
                // Create as a step
                TestStep step = new TestStep();
                step.setCaseId(caseId);
                step.setStepName("Imported from cURL");
                step.setMethod(parsed.getMethod());
                step.setUrl(parsed.getUrl());
                step.setHeaders(parsed.getHeadersAsJson());
                step.setBody(parsed.getBody());
                step.setAssertionScript(""); // Set default empty assertion script
                step.setEnabled(true);
                
                // Get max step order
                List<TestStep> existingSteps = stepMapper.findByCaseId(caseId);
                int maxOrder = existingSteps.stream()
                    .mapToInt(s -> s.getStepOrder() != null ? s.getStepOrder() : 0)
                    .max()
                    .orElse(0);
                step.setStepOrder(maxOrder + 1);
                
                step.setCreatedAt(LocalDateTime.now());
                step.setUpdatedAt(LocalDateTime.now());
                stepMapper.insert(step);
                return step;
            } else {
                // Create as a test case
                if (moduleId == null) {
                    // Create default module if not provided
                    List<TestModule> modules = moduleMapper.findByProjectId(projectId);
                    if (modules.isEmpty()) {
                        TestModule defaultModule = new TestModule();
                        defaultModule.setModuleName("Default");
                        defaultModule.setProjectId(projectId);
                        defaultModule.setCreatedAt(LocalDateTime.now());
                        defaultModule.setUpdatedAt(LocalDateTime.now());
                        moduleMapper.insert(defaultModule);
                        moduleId = defaultModule.getId();
                    } else {
                        moduleId = modules.get(0).getId();
                    }
                }
                
                TestCase testCase = new TestCase();
                testCase.setModuleId(moduleId);
                testCase.setCaseName("Imported from cURL");
                testCase.setMethod(parsed.getMethod());
                testCase.setUrl(parsed.getUrl());
                testCase.setHeaders(parsed.getHeadersAsJson());
                testCase.setBody(parsed.getBody());
                testCase.setAssertionScript(""); // Set default empty assertion script
                testCase.setIsActive(true);
                testCase.setCreatedAt(LocalDateTime.now());
                testCase.setUpdatedAt(LocalDateTime.now());
                caseMapper.insert(testCase);
                return testCase;
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to parse cURL command: " + e.getMessage(), e);
        }
    }
}

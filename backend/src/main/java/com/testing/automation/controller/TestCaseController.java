package com.testing.automation.controller;

import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestResult;
import com.testing.automation.model.TestCase;
import com.testing.automation.model.TestStep;
import com.testing.automation.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 假设我们使用 Lombok 注解，实际项目中需引入 Lombok 依赖
@RestController
@RequestMapping("/api/cases")
public class TestCaseController {

    // 注入服务层 (需要实现)
    @Autowired
    private TestCaseService testCaseService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Groovy 引擎实例 (线程安全问题需要注意，在实际应用中，通常使用线程池或引擎工厂管理)
    private final javax.script.ScriptEngine groovyEngine = new javax.script.ScriptEngineManager()
            .getEngineByName("groovy");

    // =========================================================================
    // CRUD 接口 (用例管理)
    // =========================================================================

    /**
     * 创建新用例
     * 
     * @param newCase 用例数据
     */
    @PostMapping
    public ResponseEntity<TestCase> createCase(@RequestBody TestCase newCase) {
        // Extract moduleId from nested module object if present
        if (newCase.getModuleId() == null && newCase.getModule() != null && newCase.getModule().getId() != null) {
            newCase.setModuleId(newCase.getModule().getId());
        }

        // Validate moduleId is present
        if (newCase.getModuleId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Fix bidirectional relationship: set testCase reference in each step
        if (newCase.getSteps() != null) {
            for (TestStep step : newCase.getSteps()) {
                step.setTestCase(newCase);
            }
        }
        return ResponseEntity.ok(testCaseService.save(newCase));
    }

    /**
     * 获取所有用例 (按模块筛选)
     * 
     * @param moduleId 模块 ID
     */
    @GetMapping
    public ResponseEntity<List<TestCase>> getAllCases(@RequestParam(required = false) Long moduleId) {
        if (moduleId != null) {
            return ResponseEntity.ok(testCaseService.findByModuleId(moduleId));
        }
        return ResponseEntity.ok(testCaseService.findAll());
    }

    /**
     * 按项目ID分页获取用例
     * 使用 /project 路径避免与 /{id} 冲突
     * 
     * @param projectId 项目 ID
     * @param page      页码（从0开始）
     * @param size      每页大小
     * @param keyword   搜索关键词（可选）
     */
    @GetMapping("/project")
    public ResponseEntity<Map<String, Object>> getCasesByProject(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = testCaseService.findByProjectIdWithPagination(projectId, page, size, keyword);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取单个用例
     * 注意：此路由必须在 /by-project 之后，避免路径冲突
     * 
     * @param id 用例 ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getCaseById(@PathVariable Long id) {
        TestCase testCase = testCaseService.findById(id);
        return testCase != null ? ResponseEntity.ok(testCase) : ResponseEntity.notFound().build();
    }

    /**
     * 更新用例
     * 
     * @param id          用例ID
     * @param updatedCase 更新后的用例数据
     */
    @PutMapping("/{id}")
    public ResponseEntity<TestCase> updateCase(@PathVariable Long id, @RequestBody TestCase updatedCase) {
        TestCase existingCase = testCaseService.findById(id);
        if (existingCase == null) {
            return ResponseEntity.notFound().build();
        }

        existingCase.setCaseName(updatedCase.getCaseName());
        existingCase.setMethod(updatedCase.getMethod());
        existingCase.setUrl(updatedCase.getUrl());
        existingCase.setHeaders(updatedCase.getHeaders());
        existingCase.setBody(updatedCase.getBody());
        existingCase.setPrecondition(updatedCase.getPrecondition());
        existingCase.setSetupScript(updatedCase.getSetupScript());
        existingCase.setAssertionScript(updatedCase.getAssertionScript());
        existingCase.setIsActive(updatedCase.getIsActive());

        // Extract moduleId from nested module object if present, or use direct moduleId
        if (updatedCase.getModule() != null && updatedCase.getModule().getId() != null) {
            existingCase.setModuleId(updatedCase.getModule().getId());
        } else if (updatedCase.getModuleId() != null) {
            existingCase.setModuleId(updatedCase.getModuleId());
        }

        // Validate moduleId is present
        if (existingCase.getModuleId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Handle steps update with proper bidirectional relationship
        if (updatedCase.getSteps() != null) {
            // Note: In a real app, we should be careful about orphan removal logic here
            // Simplified: we will just clear and re-add steps if needed,
            // but usually we want to update existing ones.
            // For now, let's trust the Service save() to handle it.
            existingCase.setSteps(updatedCase.getSteps());
            for (TestStep step : existingCase.getSteps()) {
                step.setCaseId(existingCase.getId());
            }
        }

        return ResponseEntity.ok(testCaseService.save(existingCase));
    }

    /**
     * 删除用例
     * 
     * @param id 用例 ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCase(@PathVariable Long id) {
        testCaseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 测试执行核心逻辑
    // =========================================================================

    /**
     * 串行执行所有用例或指定模块的用例
     * 
     * @param moduleId 可选的模块ID，用于筛选用例
     * @param envKey   环境标识 (dev/staging/prod)
     * @return 测试报告 JSON
     */
    @PostMapping("/execute")
    public ResponseEntity<List<TestResult>> executeCases(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) Long caseId,
            @RequestParam(defaultValue = "dev") String envKey) {
        return ResponseEntity.ok(testCaseService.executeAllCases(projectId, moduleId, caseId, envKey));
    }

    @GetMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) Long caseId,
            @RequestParam(defaultValue = "dev") String envKey) {
        SseEmitter emitter = new SseEmitter(-1L);

        executorService.execute(() -> {
            try {
                // Send an initial event to establish connection
                emitter.send(ScenarioExecutionEvent.builder()
                        .type("info")
                        .payload("Connecting to execution engine...")
                        .timestamp(System.currentTimeMillis())
                        .build());

                testCaseService.executeAllCases(projectId, moduleId, caseId, envKey, event -> {
                    try {
                        emitter.send(event);
                    } catch (IOException e) {
                        // ignore
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(ScenarioExecutionEvent.builder()
                            .type("error")
                            .payload(e.getMessage())
                            .timestamp(System.currentTimeMillis())
                            .build());
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    // ignore
                }
            }
        });

        return emitter;
    }

    /**
     * Dry run a test case - preview variable resolution without executing
     */
    @PostMapping("/{id}/dry-run")
    public ResponseEntity<com.testing.automation.dto.DryRunResponse> dryRunCase(
            @PathVariable Long id,
            @RequestBody com.testing.automation.dto.DryRunRequest request) {
        return ResponseEntity.ok(testCaseService.dryRunTestCase(id, request.getEnvKey()));
    }

    /**
     * 执行单个用例 (无需考虑前置依赖)
     */
    @PostMapping("/execute/{id}")
    public ResponseEntity<Map<String, Object>> executeSingleCase(@PathVariable Long id, @RequestParam String envKey) {
        // ... 逻辑类似于 executeAllCases, 但只执行一个用例 ...
        return null; // 占位符
    }

    /**
     * 核心方法：执行 Groovy 脚本（断言或变量提取）
     * * @param script Groovy 脚本内容 (例如: status_code == 200 && response.json.token !=
     * null)
     * 
     * @param context 脚本执行的上下文 (例如: 包含 status_code, response 等)
     * @return 脚本的执行结果 (例如: boolean for assertion, Object for extraction)
     */
    private Object executeGroovyScript(String script, Map<String, Object> context) {
        try {
            // 将上下文变量绑定到 Groovy 引擎
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                groovyEngine.put(entry.getKey(), entry.getValue());
            }

            // 执行脚本
            return groovyEngine.eval(script);

        } catch (Exception e) {
            // 记录脚本执行错误
            System.err.println("Groovy script execution error: " + e.getMessage());
            return null;
        }
    }
}

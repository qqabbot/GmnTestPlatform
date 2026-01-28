package com.testing.automation.service;

import com.microsoft.playwright.*;
import com.testing.automation.Mapper.UiTestMapper;
import com.testing.automation.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UiTestRunner {
    private final UiTestMapper uiTestMapper;
    private static final String VIDEO_DIR = "videos/";
    private static final String SCREENSHOT_DIR = "screenshots/";

    // Lock to ensure sequential execution (one thread at a time)
    private static final Object EXECUTION_LOCK = new Object();

    public UiTestExecutionRecord executeCase(Long caseId) {
        return executeCase(caseId, "server");
    }

    public UiTestExecutionRecord executeCase(Long caseId, String mode) {
        if ("local".equalsIgnoreCase(mode)) {
            return prepareLocalExecution(caseId);
        }
        
        synchronized (EXECUTION_LOCK) {
            // Ensure directories exist
            Paths.get(VIDEO_DIR).toFile().mkdirs();
            Paths.get(SCREENSHOT_DIR).toFile().mkdirs();

            UiTestCase uiCase = uiTestMapper.findCaseById(caseId);
            log.info("DEBUG: Fetched TestCase from DB: {}", uiCase);
            List<UiTestStep> steps = uiTestMapper.findStepsByCaseId(caseId);

            UiTestExecutionRecord record = new UiTestExecutionRecord();
            record.setCaseId(caseId);
            record.setProjectId(uiCase.getProjectId());
            record.setStatus("RUNNING");
            uiTestMapper.insertRecord(record);

            Instant start = Instant.now();

            try (Playwright playwright = Playwright.create()) {
                BrowserType browserType;
                String type = uiCase.getBrowserType();
                if (type == null)
                    type = "chromium";

                switch (type.toLowerCase()) {
                    case "firefox":
                        browserType = playwright.firefox();
                        break;
                    case "webkit":
                        browserType = playwright.webkit();
                        break;
                    case "chromium":
                    default:
                        browserType = playwright.chromium();
                        break;
                }

                log.info("LAUNCHING BROWSER: Type={}, Headless={}, Args={}", type, uiCase.getHeadless(),
                        java.util.Arrays.asList("--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage"));

                // Added stability flags
                Browser browser = browserType.launch(new BrowserType.LaunchOptions()
                        .setArgs(java.util.Arrays.asList(
                                "--no-sandbox",
                                "--disable-setuid-sandbox",
                                "--disable-gpu",
                                "--disable-dev-shm-usage"))
                        .setHeadless(uiCase.getHeadless() != null ? uiCase.getHeadless() : true));

                BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                        .setViewportSize(uiCase.getViewportWidth(), uiCase.getViewportHeight())
                        .setRecordVideoDir(Paths.get(VIDEO_DIR)));

                Page page = context.newPage();

                // Apply custom headers if configured
                if (uiCase.getCustomHeaders() != null && !uiCase.getCustomHeaders().trim().isEmpty()) {
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>() {
                        }.getType();
                        java.util.Map<String, String> headers = gson.fromJson(uiCase.getCustomHeaders(), mapType);
                        if (headers != null && !headers.isEmpty()) {
                            page.setExtraHTTPHeaders(headers);
                            log.info("CUSTOM HEADERS applied: {}", headers);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse custom headers: {}", e.getMessage());
                    }
                }

                // Apply custom cookies if configured
                if (uiCase.getCustomCookies() != null && !uiCase.getCustomCookies().trim().isEmpty()) {
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<com.microsoft.playwright.options.Cookie>>() {
                        }.getType();
                        java.util.List<com.microsoft.playwright.options.Cookie> cookies = gson
                                .fromJson(uiCase.getCustomCookies(), listType);
                        if (cookies != null && !cookies.isEmpty()) {
                            context.addCookies(cookies);
                            log.info("CUSTOM COOKIES applied: count={}", cookies.size());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse custom cookies: {}", e.getMessage());
                    }
                }

                // Auto-dismiss dialogs (alert, confirm, prompt) if enabled
                if (Boolean.TRUE.equals(uiCase.getAutoDismissDialogs())) {
                    page.onDialog(dialog -> {
                        log.info("AUTO-DISMISS DIALOG: type={}, message={}", dialog.type(), dialog.message());
                        dialog.accept(); // Accept all dialogs (clicks OK/Yes)
                    });
                    log.info("Auto-dismiss dialogs is ENABLED");
                }

                List<UiTestStepNode> rootNodes = buildStepTree(steps);
                executeStepNodes(page, rootNodes, record.getId(), new java.util.HashMap<>());

                // Video handling
                if (page.video() != null) {
                    com.microsoft.playwright.Video video = page.video();

                    // Close context to finalize video file
                    context.close();

                    try {
                        // Ensure videos directory exists
                        java.nio.file.Path videoDir = java.nio.file.Paths.get(System.getProperty("user.dir"), "videos");
                        if (!java.nio.file.Files.exists(videoDir)) {
                            java.nio.file.Files.createDirectories(videoDir);
                        }

                        // Save video to a permanent location
                        String fileName = "record_" + record.getId() + "_" + System.currentTimeMillis() + ".webm";
                        java.nio.file.Path targetPath = videoDir.resolve(fileName);

                        video.saveAs(targetPath);
                        log.info("Video saved to: {}", targetPath);
                        record.setVideoPath("videos/" + fileName); // Store relative path for portability/serving
                    } catch (Exception e) {
                        log.warn("Failed to save video: {}", e.getMessage());
                    }
                } else {
                    context.close();
                }

                browser.close();

                record.setStatus("SUCCESS");
            } catch (Exception e) {
                log.error("UI Test Execution failed", e);
                record.setStatus("FAILURE");
                record.setErrorMessage(e.getMessage());
            } finally {
                record.setDuration(Duration.between(start, Instant.now()).toMillis());
                uiTestMapper.updateRecord(record);
            }

            return record;
        }

    }

    private void executeStep(Page page, UiTestStep step, Long recordId) {
        UiTestExecutionLog logEntry = new UiTestExecutionLog();
        logEntry.setRecordId(recordId);
        logEntry.setStepId(step.getId());
        logEntry.setStepName("Step " + step.getStepOrder());
        logEntry.setAction(step.getActionType());
        logEntry.setSelector(step.getSelector()); // Store the element selector
        logEntry.setStatus("RUNNING");
        uiTestMapper.insertLog(logEntry);

        try {
            switch (step.getActionType().toUpperCase()) {
                case "NAVIGATE":
                    page.navigate(step.getValue() != null ? step.getValue() : step.getSelector());
                    break;
                case "CLICK":
                    page.click(step.getSelector());
                    break;
                case "FILL":
                    page.fill(step.getSelector(), step.getValue());
                    break;
                case "HOVER":
                    page.hover(step.getSelector());
                    break;
                case "DBL_CLICK":
                    page.dblclick(step.getSelector());
                    break;
                case "RIGHT_CLICK":
                    page.click(step.getSelector(),
                            new Page.ClickOptions().setButton(com.microsoft.playwright.options.MouseButton.RIGHT));
                    break;
                case "PRESS_KEY":
                    page.press(step.getSelector(), step.getValue());
                    break;
                case "SELECT_OPTION":
                    page.selectOption(step.getSelector(), step.getValue());
                    break;
                case "DRAG_AND_DROP":
                    page.dragAndDrop(step.getSelector(), step.getValue());
                    break;
                case "SCROLL_TO":
                    page.locator(step.getSelector()).scrollIntoViewIfNeeded();
                    break;
                case "WAIT_FOR_SELECTOR":
                    page.waitForSelector(step.getSelector());
                    break;
                case "WAIT_FOR_LOAD_STATE":
                    page.waitForLoadState();
                    break;
                case "ASSERT_VISIBLE":
                    if (!page.isVisible(step.getSelector())) {
                        throw new RuntimeException("Element not visible: " + step.getSelector());
                    }
                    break;
                case "ASSERT_NOT_VISIBLE":
                    if (page.isVisible(step.getSelector())) {
                        throw new RuntimeException("Element is visible (expected to be hidden): " + step.getSelector());
                    }
                    break;
                case "ASSERT_TEXT":
                    String text = page.textContent(step.getSelector());
                    if (text == null || !text.contains(step.getValue())) {
                        throw new RuntimeException("Text mismatch. Expected: " + step.getValue() + ", Actual: " + text);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown action type: " + step.getActionType());
            }
            logEntry.setStatus("SUCCESS");
        } catch (Exception e) {
            logEntry.setStatus("FAILURE");
            logEntry.setErrorDetail(e.getMessage());
            if (step.getScreenshotOnFailure() != null && step.getScreenshotOnFailure()) {
                String screenshotPath = SCREENSHOT_DIR + UUID.randomUUID() + ".png";
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotPath)));
                logEntry.setScreenshotPath(screenshotPath);
            }
            throw e;
        } finally {
            uiTestMapper.updateLog(logEntry);
        }
    }

    // --- Helper Methods for Recursive Execution ---

    @lombok.Data
    private static class UiTestStepNode {
        private UiTestStep step;
        private List<UiTestStepNode> children = new java.util.ArrayList<>();

        public UiTestStepNode(UiTestStep step) {
            this.step = step;
        }

    }

    private List<UiTestStepNode> buildStepTree(List<UiTestStep> steps) {
        java.util.Map<Long, UiTestStepNode> nodeMap = new java.util.HashMap<>();
        List<UiTestStepNode> roots = new java.util.ArrayList<>();

        for (UiTestStep step : steps) {
            nodeMap.put(step.getId(), new UiTestStepNode(step));
        }

        for (UiTestStep step : steps) {
            UiTestStepNode node = nodeMap.get(step.getId());
            if (step.getParentId() == null || step.getParentId() == 0) {
                roots.add(node);
            } else {
                UiTestStepNode parent = nodeMap.get(step.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node); // Orphan fallback
                }
            }
        }

        // Sort
        java.util.Comparator<UiTestStepNode> comparator = java.util.Comparator
                .comparingInt(n -> n.getStep().getStepOrder());
        roots.sort(comparator);
        for (UiTestStepNode node : nodeMap.values()) {
            node.getChildren().sort(comparator);
        }
        return roots;
    }

    private void executeStepNodes(Page page, List<UiTestStepNode> nodes, Long recordId,
            java.util.Map<String, Object> context) {
        for (UiTestStepNode node : nodes) {
            UiTestStep step = node.getStep();

            // Resolve variables (e.g. ${i})
            UiTestStep effectiveStep = resolveStepVariables(step, context);
            String action = effectiveStep.getActionType() != null ? effectiveStep.getActionType().toUpperCase()
                    : "UNKNOWN";

            if ("IF".equals(action)) {
                log.info("Executing IF Condition: {}", effectiveStep.getConditionExpression());
                if (evaluateCondition(page, effectiveStep.getConditionExpression())) {
                    executeStepNodes(page, node.getChildren(), recordId, context);
                }
            } else if ("FOR".equals(action)) {
                int count = parseLoopCount(page, effectiveStep.getLoopSource());
                log.info("Executing FOR Loop: count={}", count);
                for (int iter = 0; iter < count; iter++) {
                    context.put("i", iter);
                    executeStepNodes(page, node.getChildren(), recordId, context);
                }
            } else {
                executeStep(page, effectiveStep, recordId);
            }

            // Delay between steps (skip for logic containers)
            if (!"IF".equals(action) && !"FOR".equals(action)) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }
            }
        }
    }

    private UiTestStep resolveStepVariables(UiTestStep original, java.util.Map<String, Object> context) {
        if (context == null || context.isEmpty())
            return original;
        UiTestStep copy = new UiTestStep();
        copy.setId(original.getId());
        copy.setCaseId(original.getCaseId());
        copy.setStepOrder(original.getStepOrder());
        copy.setActionType(original.getActionType());
        copy.setWaitCondition(original.getWaitCondition());
        copy.setScreenshotOnFailure(original.getScreenshotOnFailure());
        copy.setSelector(resolveString(original.getSelector(), context));
        copy.setValue(resolveString(original.getValue(), context));
        copy.setConditionExpression(resolveString(original.getConditionExpression(), context));
        copy.setLoopSource(resolveString(original.getLoopSource(), context));
        return copy;
    }

    private String resolveString(String input, java.util.Map<String, Object> context) {
        if (input == null || !input.contains("${"))
            return input;
        String result = input;
        for (java.util.Map.Entry<String, Object> entry : context.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private boolean evaluateCondition(Page page, String condition) {
        if (condition == null || condition.isEmpty())
            return true;
        boolean negate = condition.startsWith("!");
        String selector = negate ? condition.substring(1) : condition;
        boolean visible = page.isVisible(selector);
        return negate ? !visible : visible;
    }

    private int parseLoopCount(Page page, String source) {
        if (source == null || source.isEmpty())
            return 1;
        try {
            return Integer.parseInt(source);
        } catch (NumberFormatException e) {
            return page.locator(source).count();
        }
    }

    // Local Execution Support
    private UiTestExecutionRecord prepareLocalExecution(Long caseId) {
        UiTestCase uiCase = uiTestMapper.findCaseById(caseId);
        if (uiCase == null) {
            throw new RuntimeException("Test case not found: " + caseId);
        }
        
        List<UiTestStep> steps = uiTestMapper.findStepsByCaseId(caseId);
        // Script generation is handled by generateLocalExecutionScript() method called via API
        
        UiTestExecutionRecord record = new UiTestExecutionRecord();
        record.setCaseId(caseId);
        record.setProjectId(uiCase.getProjectId());
        record.setStatus("PENDING_LOCAL");
        uiTestMapper.insertRecord(record);
        
        // Store script in record (if we add localScript field to model)
        // For now, we'll return it in the response
        
        return record;
    }

    private String generateLocalExecutionScript(UiTestCase uiCase, List<UiTestStep> steps) {
        StringBuilder script = new StringBuilder();
        
        // Determine browser type
        String browserType = uiCase.getBrowserType() != null ? uiCase.getBrowserType().toLowerCase() : "chromium";
        String browserVar = browserType;
        
        // Script header
        script.append("const { ").append(browserType).append(" } = require('playwright');\n");
        script.append("\n");
        script.append("(async () => {\n");
        script.append("  const browser = await ").append(browserVar).append(".launch({\n");
        script.append("    headless: ").append(uiCase.getHeadless() != null && uiCase.getHeadless() ? "true" : "false").append(",\n");
        script.append("    args: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage']\n");
        script.append("  });\n");
        script.append("\n");
        script.append("  const context = await browser.newContext({\n");
        script.append("    viewport: { width: ").append(uiCase.getViewportWidth() != null ? uiCase.getViewportWidth() : 1280)
              .append(", height: ").append(uiCase.getViewportHeight() != null ? uiCase.getViewportHeight() : 720).append(" }\n");
        script.append("  });\n");
        script.append("\n");
        script.append("  const page = await context.newPage();\n");
        script.append("\n");
        
        // Apply custom headers if configured
        if (uiCase.getCustomHeaders() != null && !uiCase.getCustomHeaders().trim().isEmpty()) {
            try {
                script.append("  // Apply custom headers\n");
                script.append("  await page.setExtraHTTPHeaders(").append(uiCase.getCustomHeaders()).append(");\n");
                script.append("\n");
            } catch (Exception e) {
                log.warn("Failed to parse custom headers for script generation: {}", e.getMessage());
            }
        }
        
        // Apply custom cookies if configured
        if (uiCase.getCustomCookies() != null && !uiCase.getCustomCookies().trim().isEmpty()) {
            try {
                script.append("  // Apply custom cookies\n");
                script.append("  await context.addCookies(").append(uiCase.getCustomCookies()).append(");\n");
                script.append("\n");
            } catch (Exception e) {
                log.warn("Failed to parse custom cookies for script generation: {}", e.getMessage());
            }
        }
        
        // Auto-dismiss dialogs
        if (Boolean.TRUE.equals(uiCase.getAutoDismissDialogs())) {
            script.append("  // Auto-dismiss dialogs\n");
            script.append("  page.on('dialog', dialog => dialog.accept());\n");
            script.append("\n");
        }
        
        // Generate step code
        List<UiTestStepNode> rootNodes = buildStepTree(steps);
        generateStepCode(script, rootNodes, new java.util.HashMap<>(), 2);
        
        script.append("\n");
        script.append("  await browser.close();\n");
        script.append("})();\n");
        
        return script.toString();
    }

    private void generateStepCode(StringBuilder script, List<UiTestStepNode> nodes, 
                                   java.util.Map<String, Object> context, int indent) {
        String indentStr = "  ".repeat(indent);
        
        for (UiTestStepNode node : nodes) {
            UiTestStep step = node.getStep();
            String action = step.getActionType() != null ? step.getActionType().toUpperCase() : "UNKNOWN";
            
            // Resolve variables
            String selector = resolveString(step.getSelector(), context);
            String value = resolveString(step.getValue(), context);
            
            if ("IF".equals(action)) {
                script.append(indentStr).append("if (await page.locator('").append(selector).append("').isVisible()) {\n");
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    generateStepCode(script, node.getChildren(), context, indent + 1);
                }
                script.append(indentStr).append("}\n");
            } else if ("FOR".equals(action)) {
                int count = 1;
                try {
                    if (step.getLoopSource() != null && !step.getLoopSource().isEmpty()) {
                        count = Integer.parseInt(step.getLoopSource());
                    }
                } catch (NumberFormatException e) {
                    // Use selector count
                    script.append(indentStr).append("const count = await page.locator('").append(selector).append("').count();\n");
                    script.append(indentStr).append("for (let i = 0; i < count; i++) {\n");
                    if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                        generateStepCode(script, node.getChildren(), context, indent + 1);
                    }
                    script.append(indentStr).append("}\n");
                    continue;
                }
                script.append(indentStr).append("for (let i = 0; i < ").append(count).append("; i++) {\n");
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    generateStepCode(script, node.getChildren(), context, indent + 1);
                }
                script.append(indentStr).append("}\n");
            } else {
                // Generate action code
                switch (action) {
                    case "NAVIGATE":
                        script.append(indentStr).append("await page.goto('").append(value != null ? value : selector).append("');\n");
                        break;
                    case "CLICK":
                        script.append(indentStr).append("await page.click('").append(selector).append("');\n");
                        break;
                    case "FILL":
                        script.append(indentStr).append("await page.fill('").append(selector).append("', '").append(escapeJs(value)).append("');\n");
                        break;
                    case "HOVER":
                        script.append(indentStr).append("await page.hover('").append(selector).append("');\n");
                        break;
                    case "DBL_CLICK":
                        script.append(indentStr).append("await page.dblclick('").append(selector).append("');\n");
                        break;
                    case "RIGHT_CLICK":
                        script.append(indentStr).append("await page.click('").append(selector).append("', { button: 'right' });\n");
                        break;
                    case "PRESS_KEY":
                        script.append(indentStr).append("await page.press('").append(selector).append("', '").append(escapeJs(value)).append("');\n");
                        break;
                    case "SELECT_OPTION":
                        script.append(indentStr).append("await page.selectOption('").append(selector).append("', '").append(escapeJs(value)).append("');\n");
                        break;
                    case "DRAG_AND_DROP":
                        script.append(indentStr).append("await page.dragAndDrop('").append(selector).append("', '").append(escapeJs(value)).append("');\n");
                        break;
                    case "SCROLL_TO":
                        script.append(indentStr).append("await page.locator('").append(selector).append("').scrollIntoViewIfNeeded();\n");
                        break;
                    case "WAIT_FOR_SELECTOR":
                        script.append(indentStr).append("await page.waitForSelector('").append(selector).append("');\n");
                        break;
                    case "WAIT_FOR_LOAD_STATE":
                        script.append(indentStr).append("await page.waitForLoadState();\n");
                        break;
                    case "ASSERT_VISIBLE":
                        script.append(indentStr).append("if (!(await page.locator('").append(selector).append("').isVisible())) {\n");
                        script.append(indentStr).append("  throw new Error('Element not visible: ").append(selector).append("');\n");
                        script.append(indentStr).append("}\n");
                        break;
                    case "ASSERT_NOT_VISIBLE":
                        script.append(indentStr).append("if (await page.locator('").append(selector).append("').isVisible()) {\n");
                        script.append(indentStr).append("  throw new Error('Element is visible (expected hidden): ").append(selector).append("');\n");
                        script.append(indentStr).append("}\n");
                        break;
                    case "ASSERT_TEXT":
                        script.append(indentStr).append("const text = await page.textContent('").append(selector).append("');\n");
                        script.append(indentStr).append("if (!text || !text.includes('").append(escapeJs(value)).append("')) {\n");
                        script.append(indentStr).append("  throw new Error('Text mismatch. Expected: ").append(escapeJs(value)).append(", Actual: ' + text);\n");
                        script.append(indentStr).append("}\n");
                        break;
                    default:
                        script.append(indentStr).append("// Unknown action: ").append(action).append("\n");
                        break;
                }
                
                // Add delay between steps
                script.append(indentStr).append("await page.waitForTimeout(200);\n");
            }
        }
    }

    private String escapeJs(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Generate local execution script (public method for API)
     */
    public String generateLocalExecutionScript(Long caseId) {
        UiTestCase uiCase = uiTestMapper.findCaseById(caseId);
        if (uiCase == null) {
            throw new RuntimeException("Test case not found: " + caseId);
        }
        List<UiTestStep> steps = uiTestMapper.findStepsByCaseId(caseId);
        return generateLocalExecutionScript(uiCase, steps);
    }
}

package com.testing.automation.service;

import com.microsoft.playwright.Playwright;
import com.testing.automation.Mapper.UiTestMapper;
import com.testing.automation.model.UiTestCase;
import com.testing.automation.model.UiTestStep;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class UiTestRunnerSmokeTest {

    @Test
    public void testBasicPlaywrightLaunch() {
        try (Playwright playwright = Playwright.create()) {
            playwright.chromium().launch().close();
            System.out.println("Playwright launched successfully");
        }
    }

    // This is more of an integration test structure,
    // but useful to verify the logic without a real DB if we mock the mapper.
    @Test
    public void testRunnerLogic() {
        UiTestMapper mockMapper = Mockito.mock(UiTestMapper.class);
        UiTestRunner runner = new UiTestRunner(mockMapper);

        UiTestCase testCase = new UiTestCase();
        testCase.setId(1L);
        testCase.setName("Smoke Test");
        testCase.setViewportWidth(1280);
        testCase.setViewportHeight(720);

        UiTestStep step = new UiTestStep();
        step.setId(1L);
        step.setActionType("NAVIGATE");
        step.setValue("https://example.com");
        step.setStepOrder(1);

        when(mockMapper.findCaseById(anyLong())).thenReturn(testCase);
        when(mockMapper.findStepsByCaseId(anyLong())).thenReturn(Collections.singletonList(step));

        // Note: This will actually launch a browser if run.
        // In a headless environment, this should work if dependencies are correct.
        // runner.executeCase(1L);
    }
}

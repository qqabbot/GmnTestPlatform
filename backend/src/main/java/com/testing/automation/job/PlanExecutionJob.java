package com.testing.automation.job;

import com.testing.automation.Mapper.ScheduledTaskMapper;
import com.testing.automation.model.ScheduledTask;
import com.testing.automation.service.ScenarioExecutionEngine;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

public class PlanExecutionJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(PlanExecutionJob.class);

    @Autowired
    private ScenarioExecutionEngine scenarioExecutionEngine;

    @Autowired
    private ScheduledTaskMapper taskMapper;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = context.getMergedJobDataMap().getLong("taskId");
        Long scenarioId = context.getMergedJobDataMap().getLong("scenarioId");
        String envKey = context.getMergedJobDataMap().getString("envKey");

        logger.info("Executing Scheduled Task ID: {}, Scenario ID: {}", taskId, scenarioId);

        try {
            if (scenarioId != null && scenarioId > 0) {
                logger.info("Running Scenario: {}", scenarioId);
                scenarioExecutionEngine.executeScenario(scenarioId, envKey);
            }

            // Update Last Run Time
            ScheduledTask task = taskMapper.findById(taskId);
            if (task != null) {
                task.setLastRunTime(LocalDateTime.now());
                if (context.getNextFireTime() != null) {
                    task.setNextRunTime(LocalDateTime.ofInstant(context.getNextFireTime().toInstant(),
                            java.time.ZoneId.systemDefault()));
                }
                taskMapper.update(task);
            }
        } catch (Exception e) {
            logger.error("Scheduled Task Execution Failed", e);
        }
    }
}

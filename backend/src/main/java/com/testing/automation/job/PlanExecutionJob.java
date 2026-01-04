package com.testing.automation.job;

import com.testing.automation.dto.TestResult;
import com.testing.automation.Mapper.ScheduledTaskMapper;
import com.testing.automation.model.ScheduledTask;
import com.testing.automation.service.TestPlanService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PlanExecutionJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(PlanExecutionJob.class);

    @Autowired
    private TestPlanService testPlanService;

    @Autowired
    private ScheduledTaskMapper taskMapper;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = context.getJobDetail().getJobDataMap().getLong("taskId");
        Long planId = context.getJobDetail().getJobDataMap().getLong("planId");
        String envKey = context.getJobDetail().getJobDataMap().getString("envKey");

        logger.info("Executing Scheduled Task ID: {}, Plan ID: {}", taskId, planId);

        try {
            List<TestResult> results = testPlanService.executePlan(planId, envKey);
            logger.info("Plan Execution Completed. Results: {}", results.size());

            // Update Last Run Time
            ScheduledTask task = taskMapper.findById(taskId);
            if (task != null) {
                task.setLastRunTime(LocalDateTime.now());
                task.setNextRunTime(LocalDateTime.ofInstant(context.getNextFireTime().toInstant(),
                        java.time.ZoneId.systemDefault()));
                taskMapper.update(task);
            }

            // TODO: Send Notification (Email/DingTalk)

        } catch (Exception e) {
            logger.error("Create Scheduled Task Execution Failed", e);
        }
    }
}

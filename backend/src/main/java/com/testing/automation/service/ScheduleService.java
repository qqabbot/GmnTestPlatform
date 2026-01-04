package com.testing.automation.service;

import com.testing.automation.job.PlanExecutionJob;
import com.testing.automation.Mapper.ScheduledTaskMapper;
import com.testing.automation.model.ScheduledTask;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class ScheduleService {

    @Autowired
    private Scheduler scheduler;
    @Autowired
    private ScheduledTaskMapper taskMapper;

    public List<ScheduledTask> findAll() {
        return taskMapper.findAll();
    }

    public ScheduledTask findById(Long id) {
        return taskMapper.findById(id);
    }

    public ScheduledTask create(ScheduledTask task) {
        task.setCreatedAt(LocalDateTime.now());
        task.setStatus("ACTIVE");
        taskMapper.insert(task);
        scheduleJob(task);
        return task;
    }

    public ScheduledTask update(Long id, ScheduledTask taskDetails) {
        ScheduledTask task = taskMapper.findById(id);
        if (task != null) {
            task.setName(taskDetails.getName());
            task.setCronExpression(taskDetails.getCronExpression());
            task.setPlanId(taskDetails.getPlanId());
            task.setEnvKey(taskDetails.getEnvKey());
            taskMapper.update(task);
            rescheduleJob(task);
        }
        return task;
    }

    public void delete(Long id) {
        deleteJob(id);
        taskMapper.deleteById(id);
    }

    public ScheduledTask pause(Long id) {
        ScheduledTask task = taskMapper.findById(id);
        if (task != null) {
            task.setStatus("PAUSED");
            taskMapper.update(task);
            pauseJob(id);
        }
        return task;
    }

    public ScheduledTask resume(Long id) {
        ScheduledTask task = taskMapper.findById(id);
        if (task != null) {
            task.setStatus("ACTIVE");
            taskMapper.update(task);
            resumeJob(id);
        }
        return task;
    }

    private void scheduleJob(ScheduledTask task) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(PlanExecutionJob.class)
                    .withIdentity("task_" + task.getId())
                    .usingJobData("taskId", task.getId())
                    .usingJobData("planId", task.getPlanId())
                    .usingJobData("envKey", task.getEnvKey())
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger_" + task.getId())
                    .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);

            // Update next run time
            Date nextFireTime = trigger.getNextFireTime();
            if (nextFireTime != null) {
                task.setNextRunTime(LocalDateTime.ofInstant(nextFireTime.toInstant(), ZoneId.systemDefault()));
                taskMapper.update(task);
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule job", e);
        }
    }

    private void rescheduleJob(ScheduledTask task) {
        deleteJob(task.getId());
        scheduleJob(task);
    }

    private void deleteJob(Long taskId) {
        try {
            scheduler.deleteJob(JobKey.jobKey("task_" + taskId));
        } catch (SchedulerException e) {
            // Log error
        }
    }

    private void pauseJob(Long taskId) {
        try {
            scheduler.pauseJob(JobKey.jobKey("task_" + taskId));
        } catch (SchedulerException e) {
            // Log error
        }
    }

    private void resumeJob(Long taskId) {
        try {
            scheduler.resumeJob(JobKey.jobKey("task_" + taskId));
        } catch (SchedulerException e) {
            // Log error
        }
    }
}

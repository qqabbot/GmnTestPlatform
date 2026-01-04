package com.testing.automation.Mapper;

import com.testing.automation.model.ScheduledTask;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ScheduledTaskMapper {

    @Select("SELECT * FROM scheduled_task ORDER BY id DESC")
    List<ScheduledTask> findAll();

    @Select("SELECT * FROM scheduled_task WHERE id = #{id}")
    ScheduledTask findById(Long id);

    @Select("SELECT * FROM scheduled_task WHERE status = #{status}")
    List<ScheduledTask> findByStatus(String status);

    @Insert("INSERT INTO scheduled_task (name, cron_expression, plan_id, env_key, status, last_run_time, next_run_time, created_at) "
            +
            "VALUES (#{name}, #{cronExpression}, #{planId}, #{envKey}, #{status}, #{lastRunTime}, #{nextRunTime}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduledTask task);

    @Update("UPDATE scheduled_task SET name = #{name}, cron_expression = #{cronExpression}, " +
            "plan_id = #{planId}, env_key = #{envKey}, status = #{status}, " +
            "last_run_time = #{lastRunTime}, next_run_time = #{nextRunTime} WHERE id = #{id}")
    int update(ScheduledTask task);

    @Delete("DELETE FROM scheduled_task WHERE id = #{id}")
    int deleteById(Long id);
}

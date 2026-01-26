package com.testing.automation.Mapper;

import com.testing.automation.model.ScenarioExecutionRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScenarioExecutionRecordMapper {

    @Insert("INSERT INTO scenario_execution_records (scenario_id, scenario_name, env_key, status, " +
            "total_steps, passed_steps, failed_steps, duration_ms, started_at, completed_at) " +
            "VALUES (#{scenarioId}, #{scenarioName}, #{envKey}, #{status}, #{totalSteps}, " +
            "#{passedSteps}, #{failedSteps}, #{durationMs}, #{startedAt}, #{completedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ScenarioExecutionRecord record);

    @Update("UPDATE scenario_execution_records SET status = #{status}, total_steps = #{totalSteps}, " +
            "passed_steps = #{passedSteps}, failed_steps = #{failedSteps}, duration_ms = #{durationMs}, " +
            "completed_at = #{completedAt} WHERE id = #{id}")
    void update(ScenarioExecutionRecord record);

    @Select("SELECT * FROM scenario_execution_records WHERE id = #{id}")
    ScenarioExecutionRecord findById(Long id);

    @Select("SELECT * FROM scenario_execution_records WHERE scenario_id = #{scenarioId} " +
            "ORDER BY started_at DESC LIMIT #{limit}")
    List<ScenarioExecutionRecord> findByScenarioId(@Param("scenarioId") Long scenarioId,
            @Param("limit") int limit);

    @Select("SELECT * FROM scenario_execution_records WHERE scenario_id = #{scenarioId} " +
            "AND env_key = #{envKey} ORDER BY started_at DESC LIMIT #{limit}")
    List<ScenarioExecutionRecord> findByScenarioIdAndEnv(@Param("scenarioId") Long scenarioId,
            @Param("envKey") String envKey,
            @Param("limit") int limit);

    @Delete("DELETE FROM scenario_execution_records WHERE id = #{id}")
    void deleteById(Long id);
}

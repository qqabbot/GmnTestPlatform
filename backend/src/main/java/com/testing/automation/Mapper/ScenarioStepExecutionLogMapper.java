package com.testing.automation.Mapper;

import com.testing.automation.model.ScenarioStepExecutionLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScenarioStepExecutionLogMapper {

    @Insert("INSERT INTO scenario_step_execution_logs (record_id, step_id, step_name, step_type, " +
            "status, request_url, request_method, request_headers, request_body, response_code, " +
            "response_headers, response_body, duration_ms, error_message, executed_at) " +
            "VALUES (#{recordId}, #{stepId}, #{stepName}, #{stepType}, #{status}, #{requestUrl}, " +
            "#{requestMethod}, #{requestHeaders}, #{requestBody}, #{responseCode}, #{responseHeaders}, " +
            "#{responseBody}, #{durationMs}, #{errorMessage}, #{executedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ScenarioStepExecutionLog log);

    @Select("SELECT * FROM scenario_step_execution_logs WHERE record_id = #{recordId} " +
            "ORDER BY executed_at ASC")
    List<ScenarioStepExecutionLog> findByRecordId(Long recordId);

    @Delete("DELETE FROM scenario_step_execution_logs WHERE record_id = #{recordId}")
    void deleteByRecordId(Long recordId);
}

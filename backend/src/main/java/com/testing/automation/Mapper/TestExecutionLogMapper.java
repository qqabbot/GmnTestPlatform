package com.testing.automation.Mapper;

import com.testing.automation.model.TestExecutionLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestExecutionLogMapper {

    @Select("SELECT * FROM test_execution_log WHERE case_id = #{caseId} ORDER BY id DESC LIMIT 50")
    List<TestExecutionLog> findByCaseId(Long caseId);

    @Select("SELECT * FROM test_execution_log WHERE record_id = #{recordId} ORDER BY id ASC")
    List<TestExecutionLog> findByRecordId(Long recordId);

    @Select("SELECT * FROM test_execution_log WHERE id = #{id}")
    TestExecutionLog findById(Long id);

    @Insert("INSERT INTO test_execution_log (record_id, step_name, request_url, request_method, request_headers, request_body, "
            +
            "response_status, response_headers, response_body, variable_snapshot, duration_ms, error_message, created_at) "
            +
            "VALUES (#{recordId}, #{stepName}, #{requestUrl}, #{requestMethod}, #{requestHeaders}, #{requestBody}, " +
            "#{responseStatus}, #{responseHeaders}, #{responseBody}, #{variableSnapshot}, #{durationMs}, #{errorMessage}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestExecutionLog log);

    @Delete("DELETE FROM test_execution_log WHERE id = #{id}")
    int deleteById(Long id);
}

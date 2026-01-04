package com.testing.automation.Mapper;

import com.testing.automation.model.TestExecutionRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestExecutionRecordMapper {

    @Select("SELECT * FROM test_execution_record ORDER BY id DESC")
    List<TestExecutionRecord> findAll();

    @Select("SELECT * FROM test_execution_record WHERE case_id = #{caseId} ORDER BY id DESC")
    List<TestExecutionRecord> findByCaseId(Long caseId);

    @Select("SELECT * FROM test_execution_record WHERE id = #{id}")
    TestExecutionRecord findById(Long id);

    @Select("SELECT * FROM test_execution_record WHERE project_id = #{projectId} ORDER BY id DESC")
    List<TestExecutionRecord> findByProjectId(Long projectId);

    @Select("SELECT * FROM test_execution_record WHERE module_id = #{moduleId} ORDER BY id DESC")
    List<TestExecutionRecord> findByModuleId(Long moduleId);

    @Insert("INSERT INTO test_execution_record (project_id, module_id, case_id, case_name, env_key, status, detail, duration, executed_at) "
            +
            "VALUES (#{projectId}, #{moduleId}, #{caseId}, #{caseName}, #{envKey}, #{status}, #{detail}, #{duration}, #{executedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestExecutionRecord record);

    @Delete("DELETE FROM test_execution_record WHERE id = #{id}")
    int deleteById(Long id);
}

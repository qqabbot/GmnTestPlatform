package com.testing.automation.mapper;

import com.testing.automation.model.TestCase;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestCaseMapper {

        @Select("SELECT * FROM test_case ORDER BY id DESC")
        List<TestCase> findAll();

        List<TestCase> findAllWithDetails();

        @Select("SELECT * FROM test_case WHERE id = #{id}")
        TestCase findById(Long id);

        TestCase findByIdWithDetails(Long id);

        @Select("SELECT * FROM test_case WHERE module_id = #{moduleId} ORDER BY id DESC")
        List<TestCase> findByModuleId(Long moduleId);

        List<TestCase> findByModuleIdWithDetails(Long moduleId);

        @Insert("INSERT INTO test_case (case_name, method, url, headers, body, precondition, setup_script, assertion_script, is_active, module_id, created_at, updated_at) "
                        +
                        "VALUES (#{caseName}, #{method}, #{url}, #{headers}, #{body}, #{precondition}, #{setupScript}, #{assertionScript}, #{isActive}, #{moduleId}, #{createdAt}, #{updatedAt})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(TestCase testCase);

        @Update("UPDATE test_case SET case_name = #{caseName}, method = #{method}, url = #{url}, " +
                        "headers = #{headers}, body = #{body}, precondition = #{precondition}, " +
                        "setup_script = #{setupScript}, assertion_script = #{assertionScript}, " +
                        "is_active = #{isActive}, module_id = #{moduleId}, updated_at = #{updatedAt} WHERE id = #{id}")
        int update(TestCase testCase);

        @Delete("DELETE FROM test_case WHERE id = #{id}")
        int deleteById(Long id);
}

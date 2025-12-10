package com.testing.automation.mapper;

import com.testing.automation.model.Assertion;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AssertionMapper {

    @Select("SELECT * FROM assertion WHERE test_case_id = #{testCaseId}")
    List<Assertion> findByTestCaseId(Long testCaseId);

    @Select("SELECT * FROM assertion WHERE step_id = #{stepId}")
    List<Assertion> findByStepId(Long stepId);

    @Select("SELECT * FROM assertion WHERE id = #{id}")
    Assertion findById(Long id);

    @Insert("INSERT INTO assertion (step_id, type, expression, expected_value, created_at) " +
            "VALUES (#{stepId}, #{type}, #{expression}, #{expectedValue}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Assertion assertion);

    @Update("UPDATE assertion SET step_id = #{stepId}, type = #{type}, " +
            "expression = #{expression}, expected_value = #{expectedValue} WHERE id = #{id}")
    int update(Assertion assertion);

    @Delete("DELETE FROM assertion WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM assertion WHERE test_case_id = #{testCaseId}")
    int deleteByTestCaseId(Long testCaseId);
}

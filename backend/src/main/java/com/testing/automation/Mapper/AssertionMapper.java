package com.testing.automation.mapper;

import com.testing.automation.model.Assertion;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AssertionMapper {

    @Select("SELECT * FROM assertion WHERE test_case_id = #{testCaseId}")
    List<Assertion> findByTestCaseId(Long testCaseId);

    @Select("SELECT * FROM assertion WHERE id = #{id}")
    Assertion findById(Long id);

    @Insert("INSERT INTO assertion (test_case_id, assertion_type, expected_value, actual_expression, created_at) " +
            "VALUES (#{testCaseId}, #{assertionType}, #{expectedValue}, #{actualExpression}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Assertion assertion);

    @Update("UPDATE assertion SET test_case_id = #{testCaseId}, assertion_type = #{assertionType}, " +
            "expected_value = #{expectedValue}, actual_expression = #{actualExpression} WHERE id = #{id}")
    int update(Assertion assertion);

    @Delete("DELETE FROM assertion WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM assertion WHERE test_case_id = #{testCaseId}")
    int deleteByTestCaseId(Long testCaseId);
}

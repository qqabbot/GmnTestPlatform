package com.testing.automation.mapper;

import com.testing.automation.model.TestStep;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestStepMapper {

    @Select("SELECT * FROM test_step WHERE case_id = #{caseId} ORDER BY step_order")
    List<TestStep> findByCaseId(Long caseId);

    @Select("SELECT * FROM test_step WHERE id = #{id}")
    TestStep findById(Long id);

    @Insert("INSERT INTO test_step (case_id, step_name, method, url, headers, body, assertion_script, enabled, step_order, reference_case_id, created_at, updated_at) "
            +
            "VALUES (#{caseId}, #{stepName}, #{method}, #{url}, #{headers}, #{body}, #{assertionScript}, #{enabled}, #{stepOrder}, #{referenceCaseId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestStep step);

    @Update("UPDATE test_step SET step_name = #{stepName}, method = #{method}, url = #{url}, " +
            "headers = #{headers}, body = #{body}, assertion_script = #{assertionScript}, " +
            "enabled = #{enabled}, step_order = #{stepOrder}, reference_case_id = #{referenceCaseId}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(TestStep step);

    @Delete("DELETE FROM test_step WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM test_step WHERE case_id = #{caseId}")
    int deleteByCaseId(Long caseId);
}

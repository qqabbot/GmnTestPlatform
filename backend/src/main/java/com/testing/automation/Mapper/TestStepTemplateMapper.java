package com.testing.automation.mapper;

import com.testing.automation.model.TestStepTemplate;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestStepTemplateMapper {

    @Select("SELECT * FROM test_step_template ORDER BY id DESC")
    List<TestStepTemplate> findAll();

    @Select("SELECT * FROM test_step_template WHERE id = #{id}")
    TestStepTemplate findById(Long id);

    @Select("SELECT * FROM test_step_template WHERE project_id = #{projectId} OR project_id IS NULL ORDER BY id DESC")
    List<TestStepTemplate> findByProjectIdOrGlobal(Long projectId);

    @Insert("INSERT INTO test_step_template (name, method, url, headers, body, assertion_script, project_id, created_at, updated_at) "
            +
            "VALUES (#{name}, #{method}, #{url}, #{headers}, #{body}, #{assertionScript}, #{projectId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestStepTemplate template);

    @Update("UPDATE test_step_template SET name = #{name}, method = #{method}, url = #{url}, " +
            "headers = #{headers}, body = #{body}, assertion_script = #{assertionScript}, " +
            "project_id = #{projectId}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(TestStepTemplate template);

    @Delete("DELETE FROM test_step_template WHERE id = #{id}")
    int deleteById(Long id);
}

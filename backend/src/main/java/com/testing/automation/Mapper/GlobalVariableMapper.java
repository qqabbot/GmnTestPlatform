package com.testing.automation.mapper;

import com.testing.automation.model.GlobalVariable;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GlobalVariableMapper {

        @Select("SELECT * FROM global_variable ORDER BY id DESC")
        List<GlobalVariable> findAll();

        @Select("SELECT * FROM global_variable WHERE id = #{id}")
        GlobalVariable findById(Long id);

        @Select("SELECT * FROM global_variable WHERE var_key = #{varKey}")
        GlobalVariable findByVarKey(String varKey);

        @Select("SELECT * FROM global_variable WHERE environment_id = #{envId}")
        List<GlobalVariable> findByEnvironmentId(Long envId);

        @Insert("INSERT INTO global_variable (key_name, value_content, type, description, environment_id, project_id, module_id, created_at, updated_at) "
                        +
                        "VALUES (#{keyName}, #{valueContent}, #{type}, #{description}, #{environmentId}, #{projectId}, #{moduleId}, #{createdAt}, #{updatedAt})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(GlobalVariable variable);

        @Update("UPDATE global_variable SET key_name = #{keyName}, value_content = #{valueContent}, " +
                        "type = #{type}, description = #{description}, environment_id = #{environmentId}, project_id = #{projectId}, module_id = #{moduleId}, "
                        +
                        "updated_at = #{updatedAt} WHERE id = #{id}")
        int update(GlobalVariable variable);

        @Delete("DELETE FROM global_variable WHERE id = #{id}")
        int deleteById(Long id);
}

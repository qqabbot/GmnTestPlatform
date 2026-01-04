package com.testing.automation.Mapper;

import com.testing.automation.model.GlobalVariable;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GlobalVariableMapper {

        @Select("SELECT g.*, e.id as env_id_ref, e.env_name, e.description as env_desc, e.domain " +
                        "FROM global_variable g " +
                        "LEFT JOIN environment e ON g.environment_id = e.id " +
                        "ORDER BY g.id DESC")
        @Results({
                        @Result(property = "id", column = "id"),
                        @Result(property = "keyName", column = "key_name"),
                        @Result(property = "valueContent", column = "value_content"),
                        @Result(property = "type", column = "type"),
                        @Result(property = "description", column = "description"),
                        @Result(property = "environmentId", column = "environment_id"),
                        @Result(property = "projectId", column = "project_id"),
                        @Result(property = "moduleId", column = "module_id"),
                        @Result(property = "createdAt", column = "created_at"),
                        @Result(property = "updatedAt", column = "updated_at"),
                        @Result(property = "environment.id", column = "env_id_ref"),
                        @Result(property = "environment.envName", column = "env_name"),
                        @Result(property = "environment.description", column = "env_desc"),
                        @Result(property = "environment.domain", column = "domain")
        })
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

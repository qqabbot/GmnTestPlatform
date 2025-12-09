package com.testing.automation.mapper;

import com.testing.automation.model.TestModule;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestModuleMapper {

        @Select("SELECT * FROM test_module ORDER BY id DESC")
        @Results({
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.mapper.ProjectMapper.findById"))
        })
        List<TestModule> findAll();

        @Select("SELECT * FROM test_module WHERE id = #{id}")
        @Results({
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.mapper.ProjectMapper.findById"))
        })
        TestModule findById(Long id);

        @Select("SELECT * FROM test_module WHERE project_id = #{projectId} ORDER BY id DESC")
        @Results({
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.mapper.ProjectMapper.findById"))
        })
        List<TestModule> findByProjectId(Long projectId);

        @Insert("INSERT INTO test_module (module_name, project_id, created_at, updated_at) " +
                        "VALUES (#{moduleName}, #{projectId}, #{createdAt}, #{updatedAt})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(TestModule module);

        @Update("UPDATE test_module SET module_name = #{moduleName}, project_id = #{projectId}, " +
                        "updated_at = #{updatedAt} WHERE id = #{id}")
        int update(TestModule module);

        @Delete("DELETE FROM test_module WHERE id = #{id}")
        int deleteById(Long id);
}

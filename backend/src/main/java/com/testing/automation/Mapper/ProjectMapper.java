package com.testing.automation.Mapper;

import com.testing.automation.model.Project;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProjectMapper {

        @Select("SELECT id, project_name AS projectName, description, created_at AS createdAt, updated_at AS updatedAt FROM project ORDER BY id DESC")
        List<Project> findAll();

        @Select("SELECT id, project_name AS projectName, description, created_at AS createdAt, updated_at AS updatedAt FROM project WHERE id = #{id}")
        Project findById(Long id);

        @Select("SELECT * FROM project WHERE project_name = #{projectName}")
        Project findByProjectName(String projectName);

        @Insert("INSERT INTO project (project_name, description, created_at, updated_at) " +
                        "VALUES (#{projectName}, #{description}, #{createdAt}, #{updatedAt})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(Project project);

        @Update("UPDATE project SET project_name = #{projectName}, description = #{description}, " +
                        "updated_at = #{updatedAt} WHERE id = #{id}")
        int update(Project project);

        @Delete("DELETE FROM project WHERE id = #{id}")
        int deleteById(Long id);
}

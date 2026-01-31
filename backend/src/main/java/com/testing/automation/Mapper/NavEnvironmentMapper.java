package com.testing.automation.Mapper;

import com.testing.automation.model.NavEnvironment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NavEnvironmentMapper {

    @Select("SELECT * FROM dashboard_nav_environment ORDER BY sort_order ASC, id ASC")
    List<NavEnvironment> findAll();

    @Select("SELECT * FROM dashboard_nav_environment WHERE id = #{id}")
    NavEnvironment findById(Long id);

    @Insert("INSERT INTO dashboard_nav_environment (name, description, sort_order, created_at, updated_at) " +
            "VALUES (#{name}, #{description}, #{sortOrder}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NavEnvironment env);

    @Update("UPDATE dashboard_nav_environment SET name = #{name}, description = #{description}, " +
            "sort_order = #{sortOrder}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(NavEnvironment env);

    @Delete("DELETE FROM dashboard_nav_environment WHERE id = #{id}")
    int deleteById(Long id);
}

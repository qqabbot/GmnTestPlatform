package com.testing.automation.Mapper;

import com.testing.automation.model.Environment;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface EnvironmentMapper {

    @Select("SELECT * FROM environment ORDER BY id DESC")
    List<Environment> findAll();

    @Select("SELECT * FROM environment WHERE id = #{id}")
    Environment findById(Long id);

    @Select("SELECT * FROM environment WHERE env_name = #{envName}")
    Environment findByEnvName(String envName);

    @Insert("INSERT INTO environment (env_name, domain, description, created_at, updated_at) " +
            "VALUES (#{envName}, #{domain}, #{description}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Environment environment);

    @Update("UPDATE environment SET env_name = #{envName}, domain = #{domain}, " +
            "description = #{description}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(Environment environment);

    @Delete("DELETE FROM environment WHERE id = #{id}")
    int deleteById(Long id);
}

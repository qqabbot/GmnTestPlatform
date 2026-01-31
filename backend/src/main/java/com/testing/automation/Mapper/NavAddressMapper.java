package com.testing.automation.Mapper;

import com.testing.automation.model.NavAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NavAddressMapper {

    @Select("SELECT * FROM dashboard_nav_address WHERE environment_id = #{environmentId} ORDER BY sort_order ASC, id ASC")
    List<NavAddress> findByEnvironmentId(Long environmentId);

    @Select("SELECT * FROM dashboard_nav_address WHERE id = #{id}")
    NavAddress findById(Long id);

    @Insert("INSERT INTO dashboard_nav_address (environment_id, short_name, label, url, remark, sort_order, created_at, updated_at) " +
            "VALUES (#{environmentId}, #{shortName}, #{label}, #{url}, #{remark}, #{sortOrder}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NavAddress address);

    @Update("UPDATE dashboard_nav_address SET short_name = #{shortName}, label = #{label}, url = #{url}, " +
            "remark = #{remark}, sort_order = #{sortOrder}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(NavAddress address);

    @Delete("DELETE FROM dashboard_nav_address WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM dashboard_nav_address WHERE environment_id = #{environmentId}")
    int deleteByEnvironmentId(Long environmentId);
}

package com.testing.automation.Mapper;

import com.testing.automation.model.TestPlan;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestPlanMapper {

        @Select("SELECT * FROM test_plan ORDER BY id DESC")
        @Results({
                        @Result(property = "id", column = "id"),
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.Mapper.ProjectMapper.findById")),
                        @Result(property = "testCases", column = "id", many = @Many(select = "com.testing.automation.Mapper.TestPlanMapper.findCasesByPlanId"))
        })
        List<TestPlan> findAll();

        @Select("SELECT * FROM test_plan WHERE id = #{id}")
        @Results({
                        @Result(property = "id", column = "id"),
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.Mapper.ProjectMapper.findById")),
                        @Result(property = "testCases", column = "id", many = @Many(select = "com.testing.automation.Mapper.TestPlanMapper.findCasesByPlanId"))
        })
        TestPlan findById(Long id);

        @Select("SELECT * FROM test_plan WHERE project_id = #{projectId} ORDER BY id DESC")
        @Results({
                        @Result(property = "id", column = "id"),
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.Mapper.ProjectMapper.findById")),
                        @Result(property = "testCases", column = "id", many = @Many(select = "com.testing.automation.Mapper.TestPlanMapper.findCasesByPlanId"))
        })
        List<TestPlan> findByProjectId(Long projectId);

        @Insert("INSERT INTO test_plan (name, description, project_id, created_at, updated_at) " +
                        "VALUES (#{name}, #{description}, #{projectId}, #{createdAt}, #{updatedAt})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(TestPlan plan);

        @Update("UPDATE test_plan SET name = #{name}, description = #{description}, " +
                        "project_id = #{projectId}, updated_at = #{updatedAt} WHERE id = #{id}")
        int update(TestPlan plan);

        @Delete("DELETE FROM test_plan WHERE id = #{id}")
        int deleteById(Long id);

        // ManyToMany relationship with TestCase - includes all overrides
        @Select("SELECT tc.*, " +
                        "tpc.parameter_overrides AS parameterOverrides, " +
                        "tpc.case_name_override AS caseNameOverride, " +
                        "tpc.url_override AS urlOverride, " +
                        "tpc.method_override AS methodOverride, " +
                        "tpc.headers_override AS headersOverride, " +
                        "tpc.body_override AS bodyOverride, " +
                        "tpc.assertion_script_override AS assertionScriptOverride, " +
                        "tpc.steps_override AS stepsOverride, " +
                        "tpc.enabled AS planEnabled " +
                        "FROM test_case tc " +
                        "INNER JOIN test_plan_cases tpc ON tc.id = tpc.case_id " +
                        "WHERE tpc.plan_id = #{planId} ORDER BY tpc.case_order")
        List<com.testing.automation.model.TestCase> findCasesByPlanId(Long planId);

        @Select("SELECT tpc.case_id FROM test_plan_cases tpc " +
                        "WHERE tpc.plan_id = #{planId} ORDER BY tpc.case_order")
        List<Long> findCaseIdsByPlanId(Long planId);

        @Insert("INSERT INTO test_plan_cases (plan_id, case_id, case_order, parameter_overrides) " +
                        "VALUES (#{planId}, #{caseId}, #{order}, #{overrides})")
        int addCaseToPlan(@Param("planId") Long planId, @Param("caseId") Long caseId,
                        @Param("order") Integer order, @Param("overrides") String overrides);

        @Delete("DELETE FROM test_plan_cases WHERE plan_id = #{planId}")
        int removeCasesFromPlan(Long planId);

        @Update("UPDATE test_plan_cases SET " +
                        "case_name_override = #{override.caseNameOverride}, " +
                        "url_override = #{override.urlOverride}, " +
                        "method_override = #{override.methodOverride}, " +
                        "headers_override = #{override.headersOverride}, " +
                        "body_override = #{override.bodyOverride}, " +
                        "assertion_script_override = #{override.assertionScriptOverride}, " +
                        "steps_override = #{override.stepsOverride}, " +
                        "parameter_overrides = #{override.parameterOverrides}, " +
                        "enabled = #{override.enabled} " +
                        "WHERE plan_id = #{planId} AND case_id = #{caseId}")
        int updateCaseOverrides(@Param("planId") Long planId, @Param("caseId") Long caseId,
                        @Param("override") com.testing.automation.dto.TestPlanCaseOverride override);
}

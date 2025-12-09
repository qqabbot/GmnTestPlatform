package com.testing.automation.mapper;

import com.testing.automation.model.TestPlan;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestPlanMapper {

        @Select("SELECT * FROM test_plan ORDER BY id DESC")
        @Results({
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.mapper.ProjectMapper.findById")),
                        @Result(property = "testCases", column = "id", many = @Many(select = "com.testing.automation.mapper.TestPlanMapper.findCasesByPlanId"))
        })
        List<TestPlan> findAll();

        @Select("SELECT * FROM test_plan WHERE id = #{id}")
        @Results({
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.mapper.ProjectMapper.findById")),
                        @Result(property = "testCases", column = "id", many = @Many(select = "com.testing.automation.mapper.TestPlanMapper.findCasesByPlanId"))
        })
        TestPlan findById(Long id);

        @Select("SELECT * FROM test_plan WHERE project_id = #{projectId} ORDER BY id DESC")
        @Results({
                        @Result(property = "project", column = "project_id", one = @One(select = "com.testing.automation.mapper.ProjectMapper.findById")),
                        @Result(property = "testCases", column = "id", many = @Many(select = "com.testing.automation.mapper.TestPlanMapper.findCasesByPlanId"))
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

        // ManyToMany relationship with TestCase
        @Select("SELECT tc.* FROM test_case tc " +
                        "INNER JOIN test_plan_cases tpc ON tc.id = tpc.case_id " +
                        "WHERE tpc.plan_id = #{planId} ORDER BY tpc.case_order")
        List<com.testing.automation.model.TestCase> findCasesByPlanId(Long planId);

        @Select("SELECT tc.* FROM test_case tc " +
                        "INNER JOIN test_plan_cases tpc ON tc.id = tpc.case_id " +
                        "WHERE tpc.plan_id = #{planId} ORDER BY tpc.case_order")
        List<Long> findCaseIdsByPlanId(Long planId);

        @Insert("INSERT INTO test_plan_cases (plan_id, case_id, case_order) VALUES (#{planId}, #{caseId}, #{order})")
        int addCaseToPlan(@Param("planId") Long planId, @Param("caseId") Long caseId, @Param("order") Integer order);

        @Delete("DELETE FROM test_plan_cases WHERE plan_id = #{planId}")
        int removeCasesFromPlan(Long planId);
}

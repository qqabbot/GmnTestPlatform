package com.testing.automation.Mapper;

import com.testing.automation.model.TestScenario;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TestScenarioMapper {
    List<TestScenario> findAll();

    List<TestScenario> findByProjectId(@Param("projectId") Long projectId);

    TestScenario findById(@Param("id") Long id);

    void insert(TestScenario scenario);

    void update(TestScenario scenario);

    void deleteById(@Param("id") Long id);
}

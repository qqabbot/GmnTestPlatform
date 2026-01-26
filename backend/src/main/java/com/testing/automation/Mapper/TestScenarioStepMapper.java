package com.testing.automation.Mapper;

import com.testing.automation.model.TestScenarioStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TestScenarioStepMapper {

    // Find all steps for a scenario, typically ordered by parent_id (null first)
    // and order_index
    List<TestScenarioStep> findByScenarioId(@Param("scenarioId") Long scenarioId);

    TestScenarioStep findById(@Param("id") Long id);

    void insert(TestScenarioStep step);

    void update(TestScenarioStep step);

    void deleteById(@Param("id") Long id);

    void deleteByScenarioId(@Param("scenarioId") Long scenarioId);
}

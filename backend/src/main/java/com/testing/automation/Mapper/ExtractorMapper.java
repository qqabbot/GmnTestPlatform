package com.testing.automation.mapper;

import com.testing.automation.model.Extractor;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ExtractorMapper {

    @Select("SELECT * FROM extractor WHERE test_case_id = #{testCaseId}")
    List<Extractor> findByTestCaseId(Long testCaseId);

    @Select("SELECT * FROM extractor WHERE step_id = #{stepId}")
    List<Extractor> findByStepId(Long stepId);

    @Select("SELECT * FROM extractor WHERE id = #{id}")
    Extractor findById(Long id);

    @Insert("INSERT INTO extractor (step_id, variable_name, expression, type, created_at) " +
            "VALUES (#{stepId}, #{variableName}, #{expression}, #{type}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Extractor extractor);

    @Update("UPDATE extractor SET step_id = #{stepId}, variable_name = #{variableName}, " +
            "expression = #{expression}, type = #{type} WHERE id = #{id}")
    int update(Extractor extractor);

    @Delete("DELETE FROM extractor WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM extractor WHERE test_case_id = #{testCaseId}")
    int deleteByTestCaseId(Long testCaseId);
}

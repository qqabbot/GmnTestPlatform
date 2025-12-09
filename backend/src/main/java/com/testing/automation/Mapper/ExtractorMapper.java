package com.testing.automation.mapper;

import com.testing.automation.model.Extractor;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ExtractorMapper {

    @Select("SELECT * FROM extractor WHERE test_case_id = #{testCaseId}")
    List<Extractor> findByTestCaseId(Long testCaseId);

    @Select("SELECT * FROM extractor WHERE id = #{id}")
    Extractor findById(Long id);

    @Insert("INSERT INTO extractor (test_case_id, variable_name, expression, extractor_type, created_at) " +
            "VALUES (#{testCaseId}, #{variableName}, #{expression}, #{extractorType}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Extractor extractor);

    @Update("UPDATE extractor SET test_case_id = #{testCaseId}, variable_name = #{variableName}, " +
            "expression = #{expression}, extractor_type = #{extractorType} WHERE id = #{id}")
    int update(Extractor extractor);

    @Delete("DELETE FROM extractor WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM extractor WHERE test_case_id = #{testCaseId}")
    int deleteByTestCaseId(Long testCaseId);
}

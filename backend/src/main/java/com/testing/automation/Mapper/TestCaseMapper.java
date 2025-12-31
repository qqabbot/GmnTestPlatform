package com.testing.automation.mapper;

import com.testing.automation.model.TestCase;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TestCaseMapper {

        @Select("SELECT * FROM test_case ORDER BY id DESC")
        List<TestCase> findAll();

        List<TestCase> findAllWithDetails();

        @Select("SELECT * FROM test_case WHERE id = #{id}")
        TestCase findById(Long id);

        TestCase findByIdWithDetails(Long id);

        @Select("SELECT * FROM test_case WHERE module_id = #{moduleId} ORDER BY id DESC")
        List<TestCase> findByModuleId(Long moduleId);

        List<TestCase> findByModuleIdWithDetails(Long moduleId);

        @Insert("INSERT INTO test_case (case_name, method, url, headers, body, precondition, setup_script, assertion_script, is_active, module_id, created_at, updated_at) "
                        +
                        "VALUES (#{caseName}, #{method}, #{url}, #{headers}, #{body}, #{precondition}, #{setupScript}, #{assertionScript}, #{isActive}, #{moduleId}, #{createdAt}, #{updatedAt})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(TestCase testCase);

        @Update("UPDATE test_case SET case_name = #{caseName}, method = #{method}, url = #{url}, " +
                        "headers = #{headers}, body = #{body}, precondition = #{precondition}, " +
                        "setup_script = #{setupScript}, assertion_script = #{assertionScript}, " +
                        "is_active = #{isActive}, module_id = #{moduleId}, updated_at = #{updatedAt} WHERE id = #{id}")
        int update(TestCase testCase);

        @Delete("DELETE FROM test_case WHERE id = #{id}")
        int deleteById(Long id);

        /**
         * 按模块ID列表分页查询用例（支持搜索）
         */
        @Select("<script>" +
                "SELECT tc.* FROM test_case tc " +
                "WHERE tc.module_id IN " +
                "<foreach collection='moduleIds' item='moduleId' open='(' separator=',' close=')'>" +
                "#{moduleId}" +
                "</foreach>" +
                "<if test='keyword != null and keyword != \"\"'>" +
                "AND (tc.case_name LIKE CONCAT('%', #{keyword}, '%') " +
                "OR tc.url LIKE CONCAT('%', #{keyword}, '%'))" +
                "</if>" +
                "ORDER BY tc.id DESC " +
                "LIMIT #{offset}, #{size}" +
                "</script>")
        List<TestCase> findByModuleIdsWithPagination(@Param("moduleIds") List<Long> moduleIds,
                @Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

        /**
         * 统计按模块ID列表查询的用例总数（支持搜索）
         */
        @Select("<script>" +
                "SELECT COUNT(*) FROM test_case tc " +
                "WHERE tc.module_id IN " +
                "<foreach collection='moduleIds' item='moduleId' open='(' separator=',' close=')'>" +
                "#{moduleId}" +
                "</foreach>" +
                "<if test='keyword != null and keyword != \"\"'>" +
                "AND (tc.case_name LIKE CONCAT('%', #{keyword}, '%') " +
                "OR tc.url LIKE CONCAT('%', #{keyword}, '%'))" +
                "</if>" +
                "</script>")
        int countByModuleIds(@Param("moduleIds") List<Long> moduleIds, @Param("keyword") String keyword);
}

package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.ProblemTag;

/**
 * ProblemTag Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface ProblemTagMapper {
	/**
	 * 获取全部的试题标签.
	 * @return 包含试题标签的列表
	 */
	public List<ProblemTag> getProblemTags();
	
	/**
	 * 通过试题标签的唯一标识符获取试题标签对象.
	 * @param problemTagId - 试题标签的唯一标识符
	 * @return 预期的试题标签对象或空引用
	 */
	public ProblemTag getProblemTagUsingTagId(long problemTagId);
	
	/**
	 * 通过试题的唯一标识符获取试题标签对象的列表.
	 * @param problemTagId - 试题的唯一标识符
	 * @return 预期的试题标签对象列表
	 */
	public List<ProblemTag> getProblemTagUsingProblemId(long problemId);
	
	/**
	 * 通过试题标签的唯一英文缩写获取试题标签对象.
	 * @param problemTagSlug - 试题标签的唯一英文缩写
	 * @return 预期的试题标签对象或空引用
	 */
	public ProblemTag getProblemTagUsingTagSlug(String problemTagSlug);
	
	/**
	 * 创建试题标签对象.
	 * @param problemTag - 待创建的试题标签对象
	 */
	public void createProblemTag(ProblemTag problemTag);

	/**
	 * 创建试题及试题标签的关系.
	 * @param problemId - 试题的唯一标识符
	 * @param problemTag - 试题标签对象
	 */
	public void createProblemTagRelationship(@Param(value="problemId") long problemId, @Param(value="problemTag") ProblemTag problemTag);
	
	/**
	 * 更新试题标签对象.
	 * @param problemTag - 待更新的试题标签对象
	 */
	public void updateProblemTag(ProblemTag problemTag);
	
	/**
	 * 删除试题标签对象.
	 * @param problemTagId - 待删除试题标签对象的唯一标识符
	 */
	public void deleteProblemTagUsingTagId(long problemTagId);

	/**
	 * 删除试题的全部标签.
	 * @param problemId - 试题的唯一标识符
	 */
	public void deleteProblemTagRelationship(long problemId);
}

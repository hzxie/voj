package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.trunkshell.voj.web.model.Option;

/**
 * Opton Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface OptionMapper {
	/**
	 * 获取全部系统选项.
	 * @return 一个包含全部系统选项的列表
	 */
	@Select("SELECT * FROM voj_options")
	@Options(useCache = true)
	@Results(value = {
			@Result(property = "optionId", column = "option_id"),
			@Result(property = "optionName", column = "option_name"),
			@Result(property = "optionValue", column = "option_value"),
			@Result(property = "isAutoload", column = "is_autoload")
	})
	public List<Option> getOptions();
	
	/**
	 * 获取自动加载的系统选项.
	 * @return 一个包含自动加载系统选项的列表
	 */
	@Select("SELECT * FROM voj_options WHERE is_autoload = true")
	@Options(useCache = true)
	@Results(value = {
			@Result(property = "optionId", column = "option_id"),
			@Result(property = "optionName", column = "option_name"),
			@Result(property = "optionValue", column = "option_value"),
			@Result(property = "isAutoload", column = "is_autoload")
	})
	public List<Option> getAutoloadOptions();
	
	/**
	 * 根据系统选项的名称获取选项的值.
	 * @param optionName - 系统选项的名称 
	 * @return 对应的Option对象
	 */
	@Select("SELECT * FROM voj_options WHERE option_name = #{optionName}")
	@Options(useCache = true)
	@Results(value = {
			@Result(property = "optionId", column = "option_id"),
			@Result(property = "optionName", column = "option_name"),
			@Result(property = "optionValue", column = "option_value"),
			@Result(property = "isAutoload", column = "is_autoload")
	})
	public Option getOption(@Param("optionName") String optionName);
	
	/**
	 * 更新系统选项.
	 * @param option - 系统选项对象
	 */
	@Update("UPDATE voj_options SET option_name = #{optionName}, option_value = #{optionValue}, is_autoload = #{isAutoload} WHERE option_id = #{optionId}")
	@Options(flushCache = true)
	public void updateOption(Option option);
}

/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.judger.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import org.verwandlung.voj.judger.model.JudgeResult;

/**
 * JudgeResult Data Access Object.
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface JudgeResultMapper {
	/**
	 * 通过评测结果的唯一标识符获取评测结果对象.
	 * @return 预期的评测结果对象或空引用
	 */
	@Select("SELECT * FROM voj_judge_results")
	@Options(useCache = true)
	@Results({
		 @Result(property = "judgeResultId", column = "judge_result_id"),
		 @Result(property = "judgeResultSlug", column = "judge_result_slug"),
		 @Result(property = "judgeResultName", column = "judge_result_name")
	})
	List<JudgeResult> getAllJudgeResults();
	
	/**
	 * 通过评测结果的唯一英文缩写获取评测结果对象.
	 * @param judgeResultSlug - 评测结果的唯一英文缩写
	 * @return 预期的评测结果对象或空引用
	 */
	@Select("SELECT * FROM voj_judge_results WHERE judge_result_slug = #{judgeResultSlug}")
	@Options(useCache = true)
	@Results({
		 @Result(property = "judgeResultId", column = "judge_result_id"),
		 @Result(property = "judgeResultSlug", column = "judge_result_slug"),
		 @Result(property = "judgeResultName", column = "judge_result_name")
	})
	JudgeResult getJudgeResultUsingSlug(@Param("judgeResultSlug") String judgeResultSlug);

}

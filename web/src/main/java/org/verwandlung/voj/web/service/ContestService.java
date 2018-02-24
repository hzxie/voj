package org.verwandlung.voj.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.ContestContestantMapper;
import org.verwandlung.voj.web.mapper.ContestMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.ContestContestant;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.User;

import java.util.*;

/**
 * 竞赛(Contest)的业务逻辑层.
 * @author Haozhe Xie
 */
@Service
@Transactional
public class ContestService {
	/**
	 * 获取竞赛列表.
	 * @param keyword - 竞赛的关键词
	 * @param offset - 起始竞赛的游标
	 * @param limit - 获取竞赛的数量
	 * @return 包含Contest的List对象
	 */
	public List<Contest> getContests(String keyword, long offset, int limit) {
		return contestMapper.getContests(keyword, offset, limit);
	}

	/**
	 * 通过竞赛的唯一标识符获取竞赛的详细信息.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 包含竞赛信息的Contest对象
	 */
	public Contest getContest(long contestId) {
		return contestMapper.getContestsUsingId(contestId);
	}

	/**
	 * 获取竞赛的试题列表.
	 * @param problemIdList - 包含竞赛试题ID列表的List对象
	 * @return 包含试题信息的List对象
	 */
	public List<Problem> getProblemsOfContests(List<Long> problemIdList) {
		List<Problem> problems = new ArrayList<>();
		for ( long problemId : problemIdList ) {
			Problem p = problemMapper.getProblem(problemId);

			if ( p != null ) {
				problems.add(p);
			}
		}
		return problems;
	}

	/**
	 * 获取某竞赛的参赛人数.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 某竞赛的参赛人数
	 */
	public long getNumberOfContestantsOfContest(long contestId) {
		return contestContestantMapper.getNumberOfContestantsOfContest(contestId);
	}

	/**
	 * 获取某个用户是否加入了某场竞赛.
	 * @param contestId - 竞赛的唯一标识符
	 * @param currentUser - 当前登录的用户对象
	 * @return 某个用户是否加入了某场竞赛
	 */
	public boolean isAttendContest(long contestId, User currentUser) {
		if ( currentUser == null ) {
			return false;
		}
		return contestContestantMapper.getContestantOfContest(contestId, currentUser.getUid()) != null;
	}

	/**
	 * 获取竞赛的当前状态 (未开始/进行中/已结束).
	 * @param contest - 待查询的竞赛
	 * @return 竞赛的当前状态
	 */
	private Contest.CONTEST_STATUS getContestStatus(Contest contest) {
		if ( contest == null ) {
			return null;
		}

		Date currentTime = new Date();
		if ( currentTime.before(contest.getStartTime()) ) {
			return Contest.CONTEST_STATUS.READY;
		} else if ( currentTime.after(contest.getEndTime()) ) {
			return Contest.CONTEST_STATUS.DONE;
		} else if ( currentTime.before(contest.getEndTime()) && currentTime.after(contest.getStartTime()) ) {
			return Contest.CONTEST_STATUS.LIVE;
		}
		return null;
	}

	/**
	 * 参加竞赛.
	 * @param contestId - 竞赛的唯一标识符
	 * @param currentUser - 当前登录的用户对象
	 * @return 包含是否成功参加竞赛状态信息的Map对象
	 */
	public Map<String, Boolean> attendContest(long contestId, User currentUser, boolean isCsrfTokenValid) {
		Contest contest = contestMapper.getContestsUsingId(contestId);

		Map<String, Boolean> result = new HashMap<>();
		result.put("isContestExists", contest != null);
		result.put("isContestReady", getContestStatus(contest) == Contest.CONTEST_STATUS.READY);
		result.put("isUserLogin", currentUser != null);
		result.put("isAttendedContest", isAttendContest(contestId, currentUser));
		result.put("isCsrfTokenValid", isCsrfTokenValid);

		boolean isSuccessful = result.get("isContestExists") &&  result.get("isContestReady")    &&
				               result.get("isUserLogin")     && !result.get("isAttendedContest") &&
							   result.get("isCsrfTokenValid");

		if ( isSuccessful ) {
			ContestContestant contestContestant = new ContestContestant(contest, currentUser);
			contestContestantMapper.createContestContestant(contestContestant);
		}
		result.put("isSuccessful", isSuccessful);
		return result;
	}

	/**
	 * 自动注入的ContestMapper对象.
	 */
	@Autowired
	private ContestMapper contestMapper;

	/**
	 * 自动注入的ContestContestantMapper对象.
	 */
	@Autowired
	private ContestContestantMapper contestContestantMapper;

	/**
	 * 自动注入的ProblemMapper对象.
	 * 用于获取竞赛中的试题信息.
	 */
	@Autowired
	private ProblemMapper problemMapper;
}

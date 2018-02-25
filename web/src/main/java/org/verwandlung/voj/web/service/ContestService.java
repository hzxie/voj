package org.verwandlung.voj.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.ContestContestantMapper;
import org.verwandlung.voj.web.mapper.ContestMapper;
import org.verwandlung.voj.web.mapper.ContestSubmissionMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.*;

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
		return contestMapper.getContest(contestId);
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
		Contest contest = contestMapper.getContest(contestId);

		Map<String, Boolean> result = new HashMap<>(6, 1);
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
	 * 获取OI赛制的排行榜.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 包含参赛者和提交记录信息的Map对象
	 */
	public Map<String, Object> getLeaderBoardForOi(long contestId) {
		Map<String, Object> result = new HashMap<>(3, 1);
		List<ContestContestant> contestants = contestContestantMapper.
				getContestantsOfContestForOi(contestId, 0, Integer.MAX_VALUE);
		Map<Long, Map<Long, Submission>> submissions = getSubmissionsGroupByContestant(
				contestSubmissionMapper.getAcceptedSubmissionsOfContest(contestId), true);
		rankingContestants(contestants);

		result.put("contestants", contestants);
		result.put("submissions", submissions);
		return result;
	}

	/**
	 * 获取ACM赛制的排行榜.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 包含参赛者和提交记录信息的Map对象
	 */
	public Map<String, Object> getLeaderBoardForAcm(long contestId) {
		Contest contest = contestMapper.getContest(contestId);

		Map<String, Object> result = new HashMap<>(3, 1);
		List<ContestContestant> contestants = contestContestantMapper.
				getContestantsOfContestForAcm(contestId, 0, Integer.MAX_VALUE);
		Map<Long, Map<Long, Submission>> submissions = getSubmissionsGroupByContestant(
				contestSubmissionMapper.getAcceptedSubmissionsOfContest(contestId), false);
		Collections.sort(contestants);

		// 计算罚时
		for ( ContestContestant cc : contestants ) {
			int numberOfRejected = cc.getTime();
			int penalty = numberOfRejected * 1200;
			if ( submissions.containsKey(cc.getContestant().getUid()) ) {
				Map<Long, Submission> submissionsOfContestant = submissions.get(cc.getContestant().getUid());

				for ( Map.Entry<Long, Submission> e : submissionsOfContestant.entrySet() ) {
					Submission s = e.getValue();
					long usedTimeInMilliseconds = s.getSubmitTime().getTime() - contest.getStartTime().getTime();
					s.setUsedTime((int) usedTimeInMilliseconds / 1000);
					penalty += s.getUsedTime();
				}
				cc.setTime(penalty);
			}
		}
		rankingContestants(contestants);

		result.put("contestants", contestants);
		result.put("submissions", submissions);
		return result;
	}

	/**
	 * 获取参赛者的排名.
	 * @param contestants - 竞赛参赛者列表
	 */
	public void rankingContestants(List<ContestContestant> contestants) {
		int currentRank = 1;
		if ( contestants.size() == 0 ) {
			return;
		}

		contestants.get(0).setRank(currentRank);
		for ( int i = 1; i < contestants.size(); ++ i ) {
			ContestContestant contestant = contestants.get(i);
			ContestContestant prevContestant = contestants.get(i - 1);

			if ( contestant.getScore() != prevContestant.getScore() || contestant.getTime() != prevContestant.getTime() ) {
				currentRank = i + 1;
			}
			contestant.setRank(currentRank);
		}
	}

	/**
	 * 建立竞赛提交记录的索引 (参赛者UID - 试题ID).
	 * @param contestSubmissions 包含全部竞赛提交记录的列表
	 * @param override - 当同一题出现多次提交时, 是否覆盖已有的提交记录
	 * @return 组织后的竞赛提交记录
	 */
	private Map<Long, Map<Long, Submission>> getSubmissionsGroupByContestant(
			List<ContestSubmission> contestSubmissions, boolean override) {
		Map<Long, Map<Long, Submission>> submissions = new HashMap<>();

		for ( ContestSubmission cs : contestSubmissions ) {
			long problemId = cs.getSubmission().getProblem().getProblemId();
			long contestantUid = cs.getSubmission().getUser().getUid();

			if ( !submissions.containsKey(contestantUid) ) {
				submissions.put(contestantUid, new HashMap<>());
			}
			Map<Long, Submission> submissionsOfContestant = submissions.get(contestantUid);

			if ( !override && submissionsOfContestant.containsKey(problemId) ) {
				continue;
			}
			submissionsOfContestant.put(problemId, cs.getSubmission());
		}
		return submissions;
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
	 * 自动注入的ContestSubmissionMapper对象.
	 */
	@Autowired
	private ContestSubmissionMapper contestSubmissionMapper;

	/**
	 * 自动注入的ProblemMapper对象.
	 * 用于获取竞赛中的试题信息.
	 */
	@Autowired
	private ProblemMapper problemMapper;
}

package org.verwandlung.voj.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.ContestContestantMapper;
import org.verwandlung.voj.web.mapper.ContestMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.Problem;

import java.util.List;

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
     * 获取某竞赛的参赛人数.
     * @param contestId - 竞赛的唯一标识符
     * @return 某竞赛的参赛人数
     */
    public long getNumberOfContestantsOfContest(long contestId) {
        return contestContestantMapper.getNumberOfContestantsOfContest(contestId);
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

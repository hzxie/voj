package org.verwandlung.voj.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.ContestMapper;
import org.verwandlung.voj.web.model.Contest;

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
     * 自动注入的ContestMapper对象.
     */
    @Autowired
    private ContestMapper contestMapper;
}

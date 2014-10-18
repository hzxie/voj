package com.happystudio.voj.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.happystudio.voj.dao.SubmissionDao;
import com.happystudio.voj.model.Submission;


/**
 * 评测记录Service对象. 为Controller提供服务.
 * @author Xie Haozhe
 */
@Service
@Transactional
public class SubmissionService {
	/**
	 * 根据评测记录的唯一标识符获取评测记录对象.
	 * @param submissionID - 评测记录的唯一标识符
	 * @return 评测记录对象
	 */
	public Submission getSubmission(int submissionID) {
		return submissionDao.getSubmission(submissionID);
	}
	
	/**
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<Submission> getSubmissions(int offset, int limit) {
		return submissionDao.getSubmissions(offset, limit);
	}
	
	/**
     * 自动注入的UserDAO对象.
     */
    @Autowired
    private SubmissionDao submissionDao;
}

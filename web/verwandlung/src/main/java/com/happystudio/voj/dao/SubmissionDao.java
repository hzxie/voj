package com.happystudio.voj.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.happystudio.voj.model.Submission;

/**
 * Submission类的Data Access Object.
 * @author Xie Haozhe
 */
@Repository
public class SubmissionDao {
	/**
     * 通过评测记录唯一标识符获取试题对象.
     * @param submissionID - 评测记录的唯一标识符
     * @return 一个评测记录对象的实例
     */
    @Transactional
    public Submission getSubmission(int submissionID) {
        Session session = sessionFactory.getCurrentSession();
        Submission submission = (Submission)session.get(Submission.class, submissionID);
        return submission;
    }
    
    /**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param offset - 试题唯一标识符的起始编号
     * @param limit - 每次加载评测记录的数量
     * @return 某个范围内的所有试题
     */
    public List<Submission> getSubmissions(int offset, int limit) {
    	Session session = sessionFactory.getCurrentSession();
    	@SuppressWarnings("unchecked")
		List<Submission> submissions = (List<Submission>)session.createQuery("FROM Submission WHERE submissionID >= ?0 ORDER BY submissionID DESC")
										.setParameter("0", offset)
										.setMaxResults(limit).list();
    	return submissions;
    }
    
    /**
     * 获取某个用户对某个试题的提交记录.
     * @param problemID - 试题的唯一标识符
     * @param userID - 用户的唯一标识符
     * @param limit - 每次加载评测记录的数量
     * @return 某个用户对某个试题的提交记录
     */
    public List<Submission> getSubmissionUsingProblemIDAndUserID(int problemID, int userID, int limit) { 
    	Session session = sessionFactory.getCurrentSession();
    	@SuppressWarnings("unchecked")
		List<Submission> submissions = (List<Submission>)session.createQuery("FROM Submission WHERE problem.problemID = ?0 AND user.uid = ?1 ORDER BY submissionID DESC")
										.setParameter("1", problemID)
										.setParameter("0", userID)
										.setMaxResults(limit).list();
    	return submissions;
    }
    
    /**
     * 获取某个用户通过(Accpeted)提交记录的数量.
     * @param userID - 用户的唯一标识符
     * @return 某个用户通过(Accpeted)提交记录的数量
     */
    public long getAcceptedSubmissionUsingUserID(int userID) { 
    	Session session = sessionFactory.getCurrentSession();
    	Query query = session.createQuery("SELECT COUNT(DISTINCT problem) FROM Submission WHERE user.uid = ?0 AND runtimeResult.runtimeResultSlug = ?1")
    					.setParameter("0", userID)
    					.setParameter("1", "AC");
    	return (Long)query.uniqueResult();
    }
    
    /**
     * 获取某个用户全部提交记录的数量.
     * @param userID - 用户的唯一标识符
     * @return 某个用户全部提交记录的数量
     */
    public long getTotalSubmissionUsingUserID(int userID) { 
    	Session session = sessionFactory.getCurrentSession();
    	Query query = session.createQuery("SELECT COUNT(*) FROM Submission WHERE user.uid = ?0")
    					.setParameter("0", userID);
    	return (Long)query.uniqueResult();
    }
    
	/**
     * 创建一个新的评测记录对象.
     * @param submission - 评测记录对象的实例
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean createSubmission(Submission submission) {
        Session session = sessionFactory.getCurrentSession();
        session.save(submission);
        session.flush();
        return true;
    }
    
	/**
     * 更新评测记录的信息.
     * @param submission - 评测记录对象的实例
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean updateSubmission(Submission submission) {
        Session session = sessionFactory.getCurrentSession();
        if ( session.get(Submission.class, submission.getSubmissionID()) == null ) {
            return false;
        }
        session.merge(submission);
        session.flush();
        return true;
    }
    
	/**
     * 通过评测记录的唯一标识符删除一个试题对象.
     * @param submissionID - 评测记录的唯一标识符
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean deleteSubmission(int submissionID) {
        Session session = sessionFactory.getCurrentSession();
        Submission submission = (Submission)session.get(Submission.class, submissionID);
        
        if ( submission == null ) {
            return false;
        }
        session.delete(submission);
        return true;
    }
    
    /**
     * 自动注入的SessionFactory.
     */
    @Autowired
    private SessionFactory sessionFactory;
}

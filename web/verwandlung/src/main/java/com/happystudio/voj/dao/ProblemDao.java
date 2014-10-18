package com.happystudio.voj.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.happystudio.voj.model.Problem;

/**
 * Problem类的Data Access Object.
 * @author Xie Haozhe
 */
@Repository
public class ProblemDao {
	/**
	 * 获取试题的总数量.
	 * @return 试题的总数量
	 */
	@Transactional
	public long getNumberOfProblems() {
		Session session = sessionFactory.getCurrentSession();
		Query query = session.createQuery("SELECT COUNT(*) FROM Problem WHERE isPublic = true");
		return (Long)query.uniqueResult();
	}
	
	/**
     * 通过试题唯一标识符获取试题对象.
     * @param problemID - 试题的唯一标识符
     * @return 一个试题对象的实例
     */
    @Transactional
    public Problem getProblem(int problemID) {
        Session session = sessionFactory.getCurrentSession();
        Problem problem = (Problem)session.get(Problem.class, problemID);
        return problem;
    }
    
    /**
     * 通过试题唯一标识符获取某个范围内的所有试题.
     * @param offset - 试题唯一标识符的起始编号
     * @param limit - 需要获取的试题的数量
     * @return 某个范围内的所有试题
     */
    public List<Problem> getProblems(int offset, int limit) {
    	Session session = sessionFactory.getCurrentSession();
    	@SuppressWarnings("unchecked")
		List<Problem> problems = (List<Problem>)session.createQuery("FROM Problem WHERE problemID >= ?0")
									.setParameter("0", offset)
									.setMaxResults(limit).list();
    	return problems;
    }
    
    // public List<Problem> getProblemsUsingCategory(Category category) { }
    // public List<Problem> getProblemsUsingTag(Tag tag) { }
    // public List<Problem> getProblemsUsingKeyword(String keyword) { }
    
    /**
     * 创建一个新的试题对象.
     * @param problem - 试题对象的实例
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean createProblem(Problem problem) {
        Session session = sessionFactory.getCurrentSession();
        session.save(problem);
        session.flush();
        return true;
    }
    
    /**
     * 更新试题信息.
     * @param problem - 试题对象的实例
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean updateProblem(Problem problem) {
        Session session = sessionFactory.getCurrentSession();
        if ( session.get(Problem.class, problem.getProblemID()) == null ) {
            return false;
        }
        session.merge(problem);
        session.flush();
        return true;
    }
    
    /**
     * 通过试题的唯一标识符删除一个试题对象.
     * @param problemID - 试题的唯一标识符
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean deleteProblem(int problemID) {
        Session session = sessionFactory.getCurrentSession();
        Problem problem = (Problem)session.get(Problem.class, problemID);
        
        if ( problem == null ) {
            return false;
        }
        session.delete(problem);
        return true;
    }
    
    /**
     * 自动注入的SessionFactory.
     */
    @Autowired
    private SessionFactory sessionFactory;
}

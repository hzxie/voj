package com.trunkshell.voj.web.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.trunkshell.voj.web.mapper.DiscussionTopicMapper;
import com.trunkshell.voj.web.model.DiscussionTopic;

/**
 * 
 * @author Luo Guofu
 */
public class DiscussionTopicService {
	
	private List<DiscussionTopic> getChiledren(List<DiscussionTopic> data, Integer parentId) {
		List<DiscussionTopic> children = new ArrayList<DiscussionTopic>();
		for (DiscussionTopic t : data) {
			if (t.getTopicParentId() == parentId) {
				children.add(t);
			}
		}
		return children;
	}
	
	public List<DiscussionTopic> getAllDiscussionTopic() {
		List<DiscussionTopic> topics = discussionTopicMapper.getDiscussionTopics();
		
		
		return null;
	}

	@Autowired
	private DiscussionTopicMapper discussionTopicMapper;
}

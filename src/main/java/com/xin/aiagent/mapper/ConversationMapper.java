package com.xin.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.aiagent.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}

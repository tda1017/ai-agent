package com.xin.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.aiagent.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}

package com.xin.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.aiagent.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

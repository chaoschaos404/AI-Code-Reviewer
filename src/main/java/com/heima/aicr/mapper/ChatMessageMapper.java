package com.heima.aicr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.aicr.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}

package com.heima.aicr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.aicr.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 聊天会话Mapper接口
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 根据sessionId查询会话
     */
    @Select("SELECT * FROM chat_session WHERE session_id = #{sessionId} AND deleted = 0")
    ChatSession selectBySessionId(@Param("sessionId") String sessionId);
}

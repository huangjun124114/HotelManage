package com.linxi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.entity.MessageNotifyLog;

import java.util.List;
import java.util.Map;

public interface MessageNotifyLogService {

    /**
     * 获取当前用户的消息列表（分页）
     */
    Page<MessageNotifyLog> getMyMessages(Long userId, int pageNum, int pageSize);

    /**
     * 获取当前用户未读消息数
     */
    long getUnreadCount(Long userId);

    /**
     * 标记消息已读
     */
    boolean markRead(Long id, Long userId);

    /**
     * 全部标记已读
     */
    boolean markAllRead(Long userId);

    /**
     * 发送站内消息
     */
    MessageNotifyLog sendMessage(MessageNotifyLog message);


    /**
     * 获取最近N条未读消息
     */
    List<MessageNotifyLog> getRecentUnread(Long userId, int limit);
}

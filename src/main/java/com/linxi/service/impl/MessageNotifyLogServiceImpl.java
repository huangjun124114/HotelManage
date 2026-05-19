package com.linxi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.entity.MessageNotifyLog;
import com.linxi.mapper.MessageNotifyLogMapper;
import com.linxi.service.MessageNotifyLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class MessageNotifyLogServiceImpl implements MessageNotifyLogService {

    @Autowired
    private MessageNotifyLogMapper messageNotifyLogMapper;

    @Override
    public Page<MessageNotifyLog> getMyMessages(Long userId, int pageNum, int pageSize) {
        Page<MessageNotifyLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MessageNotifyLog> wrapper = new LambdaQueryWrapper<MessageNotifyLog>()
                .eq(MessageNotifyLog::getReceiverUserId, userId)
                .eq(MessageNotifyLog::getSendStatus, 1)
                .orderByDesc(MessageNotifyLog::getCreateTime);
        return messageNotifyLogMapper.selectPage(page, wrapper);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return messageNotifyLogMapper.selectCount(
            new LambdaQueryWrapper<MessageNotifyLog>()
                .eq(MessageNotifyLog::getReceiverUserId, userId)
                .eq(MessageNotifyLog::getSendStatus, 1)
                .eq(MessageNotifyLog::getIsRead, 0)
        );
    }

    @Override
    public boolean markRead(Long id, Long userId) {
        LambdaUpdateWrapper<MessageNotifyLog> wrapper = new LambdaUpdateWrapper<MessageNotifyLog>()
                .eq(MessageNotifyLog::getId, id)
                .eq(MessageNotifyLog::getReceiverUserId, userId)
                .set(MessageNotifyLog::getIsRead, 1);
        return messageNotifyLogMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean markAllRead(Long userId) {
        LambdaUpdateWrapper<MessageNotifyLog> wrapper = new LambdaUpdateWrapper<MessageNotifyLog>()
                .eq(MessageNotifyLog::getReceiverUserId, userId)
                .eq(MessageNotifyLog::getIsRead, 0)
                .set(MessageNotifyLog::getIsRead, 1);
        return messageNotifyLogMapper.update(null, wrapper) >= 0;
    }

    @Override
    public MessageNotifyLog sendMessage(MessageNotifyLog message) {
        message.setChannel("site");
        message.setSendStatus(1);
        message.setIsRead(0);
        message.setSendTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        messageNotifyLogMapper.insert(message);
        return message;
    }

    @Override
    public List<MessageNotifyLog> sendBatchMessages(List<MessageNotifyLog> messages) {
        for (MessageNotifyLog msg : messages) {
            sendMessage(msg);
        }
        return messages;
    }

    @Override
    public List<MessageNotifyLog> getRecentUnread(Long userId, int limit) {
        LambdaQueryWrapper<MessageNotifyLog> wrapper = new LambdaQueryWrapper<MessageNotifyLog>()
                .eq(MessageNotifyLog::getReceiverUserId, userId)
                .eq(MessageNotifyLog::getSendStatus, 1)
                .orderByDesc(MessageNotifyLog::getCreateTime)
                .last("LIMIT " + limit);
        return messageNotifyLogMapper.selectList(wrapper);
    }
}

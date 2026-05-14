package com.linxi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("message_notify_log")
public class MessageNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String notifyType;
    private String channel;
    private Long receiverUserId;
    private String receiverPhone;
    private String receiverName;
    private Long storeId;
    private String title;
    private String content;
    private Integer sendStatus;
    private String failReason;
    private String sendTime;
    private String createTime;
}

package com.linxi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linxi.common.Result;
import com.linxi.entity.MessageNotifyLog;
import com.linxi.entity.SysUser;
import com.linxi.mapper.SysUserMapper;
import com.linxi.mapper.SysUserStoreMapper;
import com.linxi.entity.SysUserStore;
import com.linxi.service.MessageNotifyLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageNotifyLogController {

    @Autowired
    private MessageNotifyLogService messageNotifyLogService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserStoreMapper sysUserStoreMapper;

    /**
     * 获取当前用户的消息列表（分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getMyMessages(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getCurrentUserId();
        Page<MessageNotifyLog> page = messageNotifyLogService.getMyMessages(userId, pageNum, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.success(result);
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount() {
        Long userId = getCurrentUserId();
        long count = messageNotifyLogService.getUnreadCount(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        return Result.success(result);
    }

    /**
     * 获取最近未读消息（用于铃铛弹窗）
     */
    @GetMapping("/recent")
    public Result<List<MessageNotifyLog>> getRecentUnread(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = getCurrentUserId();
        List<MessageNotifyLog> messages = messageNotifyLogService.getRecentUnread(userId, limit);
        return Result.success(messages);
    }

    /**
     * 标记消息已读
     */
    @PutMapping("/{id}/read")
    public Result<Boolean> markRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean success = messageNotifyLogService.markRead(id, userId);
        return Result.success(success);
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read-all")
    public Result<Boolean> markAllRead() {
        Long userId = getCurrentUserId();
        boolean success = messageNotifyLogService.markAllRead(userId);
        return Result.success(success);
    }

    /**
     * 发送站内消息给指定用户（内部调用）
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('SUPER_ADMIN')")
    public Result<MessageNotifyLog> sendMessage(@RequestBody MessageNotifyLog message) {
        MessageNotifyLog saved = messageNotifyLogService.sendMessage(message);
        return Result.success(saved);
    }

    /**
     * 发送提醒给指定门店的店长
     */
    @PostMapping("/notify-store")
    @PreAuthorize("hasRole('USER') or hasRole('SUPER_ADMIN')")
    public Result<Map<String, Object>> notifyStore(@RequestBody Map<String, Object> params) {
        Long storeId = Long.valueOf(params.get("storeId").toString());
        String title = (String) params.getOrDefault("title", "日报填报提醒");
        String content = (String) params.getOrDefault("content", "您有未填报的日报，请尽快完成填报。");

        // 查找门店关联的店长用户
        List<SysUserStore> userStores = sysUserStoreMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserStore>()
                .eq(SysUserStore::getStoreId, storeId)
        );

        int sentCount = 0;
        for (SysUserStore us : userStores) {
            SysUser user = sysUserMapper.selectById(us.getUserId());
            if (user != null) {
                MessageNotifyLog msg = new MessageNotifyLog();
                msg.setNotifyType("daily_report");
                msg.setReceiverUserId(user.getId());
                msg.setReceiverPhone(user.getPhone());
                msg.setReceiverName(user.getRealName());
                msg.setStoreId(storeId);
                msg.setTitle(title);
                msg.setContent(content);
                messageNotifyLogService.sendMessage(msg);
                sentCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sentCount", sentCount);
        return Result.success(result);
    }

    /**
     * 一键提醒所有未填报门店
     */
    @PostMapping("/notify-unfilled")
    @PreAuthorize("hasRole('USER') or hasRole('SUPER_ADMIN')")
    public Result<Map<String, Object>> notifyUnfilled(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stores = (List<Map<String, Object>>) params.get("stores");
        String title = (String) params.getOrDefault("title", "日报填报提醒");
        String content = (String) params.getOrDefault("content", "您有未填报的日报，请尽快完成填报。");

        int sentCount = 0;
        if (stores != null) {
            for (Map<String, Object> store : stores) {
                Long storeId = Long.valueOf(store.get("storeId").toString());
                List<SysUserStore> userStores = sysUserStoreMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserStore>()
                        .eq(SysUserStore::getStoreId, storeId)
                );
                for (SysUserStore us : userStores) {
                    SysUser user = sysUserMapper.selectById(us.getUserId());
                    if (user != null) {
                        MessageNotifyLog msg = new MessageNotifyLog();
                        msg.setNotifyType("daily_report");
                        msg.setReceiverUserId(user.getId());
                        msg.setReceiverPhone(user.getPhone());
                        msg.setReceiverName(user.getRealName());
                        msg.setStoreId(storeId);
                        msg.setTitle(title);
                        msg.setContent(content);
                        messageNotifyLogService.sendMessage(msg);
                        sentCount++;
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sentCount", sentCount);
        return Result.success(result);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SysUser) {
            return ((SysUser) auth.getPrincipal()).getId();
        }
        // 从认证信息中获取用户名再查ID
        String username = auth.getName();
        SysUser user = sysUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1")
        );
        return user != null ? user.getId() : null;
    }
}

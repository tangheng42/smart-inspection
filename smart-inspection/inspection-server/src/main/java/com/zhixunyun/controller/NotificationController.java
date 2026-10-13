package com.zhixunyun.controller;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.dto.NotificationQueryDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.NotificationService;
import com.zhixunyun.vo.NotificationVO;
import com.zhixunyun.vo.UnreadCountVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "12. 消息通知")
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    @RequireRole({"ADMIN", "INSPECTOR", "MAINTAINER"})
    public Result<PageResult<NotificationVO>> page(@Valid NotificationQueryDTO dto) {
        return Result.success(service.pageQuery(dto));
    }

    @GetMapping("/unread-count")
    @RequireRole({"ADMIN", "INSPECTOR", "MAINTAINER"})
    public Result<UnreadCountVO> unreadCount() {
        return Result.success(service.getUnreadCount());
    }

    @PatchMapping("/{notificationId}/read")
    @RequireRole({"ADMIN", "INSPECTOR", "MAINTAINER"})
    public Result<Void> markRead(@PathVariable Long notificationId) {
        service.markRead(notificationId);
        return Result.success();
    }

    @PatchMapping("/read-all")
    @RequireRole({"ADMIN", "INSPECTOR", "MAINTAINER"})
    public Result<Void> markAllRead() {
        service.markAllRead();
        return Result.success();
    }
}

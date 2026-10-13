package com.zhixunyun.service;

import com.zhixunyun.dto.NotificationQueryDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.NotificationVO;
import com.zhixunyun.vo.UnreadCountVO;

public interface NotificationService {
    PageResult<NotificationVO> pageQuery(NotificationQueryDTO dto);
    UnreadCountVO getUnreadCount();
    void markRead(Long notificationId);
    void markAllRead();
}

package com.zhixunyun.mapper;

import java.util.List;
import com.zhixunyun.dto.NotificationQueryDTO;
import com.zhixunyun.entity.Notification;
import com.zhixunyun.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
    List<NotificationVO> pageQuery(@Param("dto") NotificationQueryDTO dto,
                                   @Param("recipientId") Long recipientId);
    long countUnread(Long recipientId);
    Long getRecipientId(Long notificationId);
    int markRead(@Param("notificationId") Long notificationId,
                 @Param("recipientId") Long recipientId);
    int markAllRead(Long recipientId);
    int insertOrRefresh(Notification notification);
}

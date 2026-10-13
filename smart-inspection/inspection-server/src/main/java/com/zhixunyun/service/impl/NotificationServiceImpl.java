package com.zhixunyun.service.impl;

import java.util.List;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.NotificationQueryDTO;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.NotificationMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.NotificationService;
import com.zhixunyun.vo.NotificationVO;
import com.zhixunyun.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 站内通知查询与已读管理；每次查询和更新都按当前收件人限制数据范围。 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper mapper;

    @Override
    public PageResult<NotificationVO> pageQuery(NotificationQueryDTO dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<NotificationVO> records = mapper.pageQuery(dto, BaseContext.getCurrentId());
        return new PageResult<>(dto.getPageNum(), dto.getPageSize(),
                new PageInfo<>(records).getTotal(), records);
    }

    @Override
    public UnreadCountVO getUnreadCount() {
        return new UnreadCountVO(mapper.countUnread(BaseContext.getCurrentId()));
    }

    @Override
    public void markRead(Long notificationId) {
        Long recipientId = mapper.getRecipientId(notificationId);
        if (recipientId == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (!BaseContext.getCurrentId().equals(recipientId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // SQL 只更新未读记录；已经读过时 0 行受影响仍属于幂等成功。
        mapper.markRead(notificationId, recipientId);
    }

    @Override
    public void markAllRead() {
        mapper.markAllRead(BaseContext.getCurrentId());
    }
}

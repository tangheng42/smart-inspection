package com.zhixunyun.enumeration;

public enum WorkOrderOperationType {
    CREATE,// 创建
    ASSIGN,// 分配
    REASSIGN,// 重新分配
    ACCEPT,// 接收
    SUBMIT_RESULT,// 提交结果
    VERIFY_PASS,// 验证通过
    VERIFY_REJECT,// 验证拒绝
    REWORK,// 重新处理
    CANCEL// 取消
}

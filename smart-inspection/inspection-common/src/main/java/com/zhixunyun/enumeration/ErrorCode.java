package com.zhixunyun.enumeration;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_VALIDATION_FAILED(10001, "请求参数校验失败"),
    USERNAME_OR_PASSWORD_ERROR(11001, "用户名或密码错误"),
    USER_DISABLED(11002, "用户账号已被禁用"),
    TOKEN_MISSING(11003, "请求未携带 Token"),
    TOKEN_INVALID(11004, "Token 无效或已过期"),
    FORBIDDEN(11005, "当前用户无权执行该操作"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    USER_NOT_FOUND(11006, "用户不存在"),
    USERNAME_EXISTS(11007, "用户名已存在"),
    CANNOT_DISABLE_SELF(11008, "不能禁用自己"),


    AREA_EXIST_CODE(13001, "区域编码已存在"),
    AREA_NOT_FOUND(13002, "区域不存在"),
    AREA_HAS_EQUIPMENT(13003, "区域下还有设备，不能停用"),

    EQUIPMENT_TYPE_CODE_EXISTS(13101, "设备类型编码已存在"),
    EQUIPMENT_TYPE_NOT_FOUND(13102, "设备类型不存在"),
    EQUIPMENT_TYPE_HAS_ENABLED_EQUIPMENT(13103, "设备类型下还有启用设备，不能停用"),

    EQUIPMENT_CODE_EXISTS(13201, "设备编码已存在"),
    EQUIPMENT_SERIAL_NUMBER_EXISTS(13202, "设备序列号已存在"),
    EQUIPMENT_NOT_FOUND(13203, "设备不存在"),
    EQUIPMENT_TYPE_UNAVAILABLE(13204, "设备类型不存在或已停用"),
    EQUIPMENT_AREA_UNAVAILABLE(13205, "所属区域不存在或已停用"),
    EQUIPMENT_REFERENCED(13206, "设备已被业务引用，不能删除"),
    EQUIPMENT_STATUS_CHANGE_FORBIDDEN(13207, "当前设备状态不能通过管理接口修改"),

    INSPECTION_ITEM_CODE_EXISTS(13301, "巡检项目编码已存在"),
    INSPECTION_ITEM_NOT_FOUND(13302, "巡检项目不存在"),
    INSPECTION_ITEM_TYPE_UNAVAILABLE(13303, "设备类型不存在或已停用"),
    INSPECTION_ITEM_RESULT_CONFIG_INVALID(13304, "巡检项目结果配置不合法"),
    EQUIPMENT_STATUS_INVALID(13305, "设备状态无效"),
    INSPECTION_ITEM_DISABLED(13306, "巡检项目已被停用"),

    INSPECTION_PLAN_NAME_EXISTS(13401, "巡检计划名称已存在"),
    INSPECTION_PLAN_NOT_FOUND(13402, "巡检计划不存在"),
    INSPECTION_PLAN_CONFLICT(13403, "巡检计划与已有计划存在时间冲突"),
    INSPECTION_PLAN_EQUIPMENT_TYPE_MISMATCH(13404, "巡检计划中的设备类型不一致"),
    INSPECTION_PLAN_ITEM_TYPE_MISMATCH(13405, "巡检项目与设备类型不匹配"),
    INSPECTION_PLAN_INSPECTOR_UNAVAILABLE(13406, "所选用户不是可用的巡检员"),
    INSPECTION_PLAN_SCHEDULE_INVALID(13407, "巡检计划周期配置不合法"),
    INSPECTION_PLAN_NO_FUTURE_EXECUTION(13408, "计划有效期内没有可执行时间"),
    INSPECTION_PLAN_HAS_TASKS(13409, "巡检计划已生成任务，不能删除"),

    INSPECTION_TASK_NOT_FOUND(13501, "巡检任务不存在"),
    INSPECTION_TASK_STATUS_CONFLICT(13502, "巡检任务状态冲突"),
    INSPECTION_TASK_ALREADY_STARTED(13503, "巡检任务已启动"),
    INSPECTION_TASK_EQUIPMENT_INVALID(13504, "巡检结果中的设备与任务不匹配"),
    INSPECTION_TASK_ITEM_INVALID(13505, "巡检项目结果与任务快照不匹配"),
    INSPECTION_TASK_RESULT_INCONSISTENT(13506, "设备整体结果与巡检项目结果不一致"),
    INSPECTION_TASK_FAULT_INVALID(13507, "异常设备必须填写异常信息，正常设备不能填写异常信息"),
    INSPECTION_TASK_ATTACHMENT_INVALID(13508, "图片尚未上传或图片地址无效"),

    INSPECTION_RECORD_NOT_FOUND(13601, "巡检记录不存在"),

    FAULT_RECORD_NOT_FOUND(13701, "异常记录不存在"),

    WORK_ORDER_NOT_FOUND(13801, "维修工单不存在"),
    WORK_ORDER_STATUS_CONFLICT(13802, "维修工单状态冲突"),
    WORK_ORDER_MAINTAINER_UNAVAILABLE(13803, "所选用户不是可用的维修员"),
    WORK_ORDER_DUE_AT_INVALID(13804, "工单截止时间必须晚于当前时间"),
    WORK_ORDER_ATTACHMENT_INVALID(13805, "维修图片尚未上传、无效或不属于当前用户"),

    NOTIFICATION_NOT_FOUND(13901, "通知不存在"),

    FILE_INVALID(14001, "仅支持有效的 JPEG、PNG 或 WEBP 图片"),
    FILE_TOO_LARGE(14002, "图片大小不能超过10MB"),
    FILE_STORAGE_ERROR(14003, "图片存储失败");



    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

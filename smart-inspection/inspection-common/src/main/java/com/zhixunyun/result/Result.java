package com.zhixunyun.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Result", description = "智巡云统一响应结构；code 为 0 表示成功，非 0 表示业务失败")
public class Result<T> {

    @Schema(description = "业务状态码", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer code;

    @Schema(description = "响应提示信息", example = "success", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "响应数据；无返回数据或请求失败时可能为空", nullable = true)
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<T>();
        result.code = 0;
        result.message = "success";
        result.data = data;
        return result;
    }
    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.code = 0;
        result.message = "success";
        return result;
    }
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        return result;
    }

}

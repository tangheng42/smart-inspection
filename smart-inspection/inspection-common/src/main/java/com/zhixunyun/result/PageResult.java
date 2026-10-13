package com.zhixunyun.result;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private Integer pageNum;

    private Integer pageSize;

    private Long total;

    private List<T> records;
}

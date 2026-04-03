package com.epass.food.modules.system.operationlog.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogListQuery extends PageQuery {

    private String module;

    private String action;

    private String username;

    private Integer success;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTimeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTimeEnd;
}

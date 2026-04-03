package com.epass.food.modules.food.app.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppMenuItemListQuery extends PageQuery {

    private String name;

    private Long categoryId;
}

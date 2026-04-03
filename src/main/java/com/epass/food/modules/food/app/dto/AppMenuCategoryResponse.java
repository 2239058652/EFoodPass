package com.epass.food.modules.food.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class AppMenuCategoryResponse {

    private Long id;

    private String name;

    private Integer sortNo;

    private List<AppMenuItemResponse> items;
}

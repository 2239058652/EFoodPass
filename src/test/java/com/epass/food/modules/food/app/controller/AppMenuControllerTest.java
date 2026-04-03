package com.epass.food.modules.food.app.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.app.dto.AppMenuCategoryResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemDetailResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemResponse;
import com.epass.food.modules.food.app.service.AppMenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppMenuControllerTest {

    @Mock
    private AppMenuService appMenuService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        AppMenuController controller = new AppMenuController(appMenuService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void treeShouldReturnMenuTree() throws Exception {
        AppMenuItemResponse item = new AppMenuItemResponse();
        item.setId(101L);
        item.setName("Fried Rice");
        item.setPrice(new BigDecimal("18.00"));

        AppMenuCategoryResponse category = new AppMenuCategoryResponse();
        category.setId(1L);
        category.setName("Main");
        category.setSortNo(1);
        category.setItems(List.of(item));

        when(appMenuService.listMenuTree()).thenReturn(List.of(category));

        mockMvc.perform(get("/app/menu/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("Main"))
                .andExpect(jsonPath("$.data[0].items[0].name").value("Fried Rice"));

        verify(appMenuService).listMenuTree();
    }

    @Test
    void itemsShouldPassQueryToService() throws Exception {
        AppMenuItemResponse item = new AppMenuItemResponse();
        item.setId(102L);
        item.setName("Noodles");

        PageResult<AppMenuItemResponse> pageResult = new PageResult<>();
        pageResult.setTotal(1L);
        pageResult.setPageNum(2L);
        pageResult.setPageSize(5L);
        pageResult.setRecords(List.of(item));

        when(appMenuService.listAvailableItems(argThat(query ->
                query != null
                        && "noodle".equals(query.getName())
                        && Long.valueOf(3L).equals(query.getCategoryId())
                        && Long.valueOf(2L).equals(query.getPageNum())
                        && Long.valueOf(5L).equals(query.getPageSize())
        ))).thenReturn(pageResult);

        mockMvc.perform(get("/app/menu/items")
                        .param("name", "noodle")
                        .param("categoryId", "3")
                        .param("pageNum", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("Noodles"));
    }

    @Test
    void itemDetailShouldReturnDetail() throws Exception {
        AppMenuItemDetailResponse detail = new AppMenuItemDetailResponse();
        detail.setId(103L);
        detail.setName("Soup");
        detail.setCategoryName("Soup");
        detail.setPrice(new BigDecimal("8.50"));

        when(appMenuService.getAvailableItemDetail(103L)).thenReturn(detail);

        mockMvc.perform(get("/app/menu/item/103"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Soup"))
                .andExpect(jsonPath("$.data.categoryName").value("Soup"));

        verify(appMenuService).getAvailableItemDetail(103L);
    }
}

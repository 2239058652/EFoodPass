package com.epass.food.modules.food.order.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.modules.food.order.dto.*;
import com.epass.food.modules.food.order.service.FoodOrderService;
import com.epass.food.modules.system.operationlog.annotation.OperationLog;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/food/order")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    public FoodOrderController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    @PreAuthorize("hasAuthority('food:order:list')")
    @GetMapping("/list")
    public Result<PageResult<FoodOrderListResponse>> list(FoodOrderListQuery query) {
        return Result.success(foodOrderService.listOrders(query));
    }

    @PreAuthorize("hasAuthority('food:order:export')")
    @GetMapping("/export")
    @OperationLog(module = "FOOD_ORDER", action = "EXPORT")
    public ResponseEntity<byte[]> export(FoodOrderListQuery query) {
        String filename = "orders-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(foodOrderService.exportOrders(query));
    }

    @PreAuthorize("hasAuthority('food:order:detail')")
    @GetMapping("/{id}")
    public Result<FoodOrderDetailResponse> detail(@PathVariable Long id) {
        return Result.success(foodOrderService.getOrderDetail(id));
    }

    @PreAuthorize("hasAuthority('food:order:add')")
    @PostMapping
    @OperationLog(module = "FOOD_ORDER", action = "CREATE")
    public Result<Void> create(@Valid @RequestBody FoodOrderCreateRequest request) {
        foodOrderService.createOrder(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:order:process')")
    @PutMapping("/process")
    @OperationLog(module = "FOOD_ORDER", action = "PROCESS")
    public Result<Void> process(@Valid @RequestBody FoodOrderUpdateStatusRequest request) {
        foodOrderService.processOrder(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:order:cancel')")
    @PutMapping("/cancel")
    @OperationLog(module = "FOOD_ORDER", action = "CANCEL")
    public Result<Void> cancel(@Valid @RequestBody FoodOrderUpdateStatusRequest request) {
        foodOrderService.cancelOrder(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:order:refund')")
    @PutMapping("/refund")
    @OperationLog(module = "FOOD_ORDER", action = "REFUND")
    public Result<Void> refund(@Valid @RequestBody FoodOrderUpdateStatusRequest request) {
        foodOrderService.refundOrder(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:order:complete')")
    @PutMapping("/complete")
    @OperationLog(module = "FOOD_ORDER", action = "COMPLETE")
    public Result<Void> complete(@Valid @RequestBody FoodOrderUpdateStatusRequest request) {
        foodOrderService.completeOrder(request);
        return Result.success();
    }
}

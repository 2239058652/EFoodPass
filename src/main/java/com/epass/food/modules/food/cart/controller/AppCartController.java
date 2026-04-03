package com.epass.food.modules.food.cart.controller;

import com.epass.food.common.result.Result;
import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.food.cart.dto.AppCartAddItemRequest;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutRequest;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutResponse;
import com.epass.food.modules.food.cart.dto.AppCartResponse;
import com.epass.food.modules.food.cart.dto.AppCartUpdateItemRequest;
import com.epass.food.modules.food.cart.service.AppCartService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/cart")
public class AppCartController {

    private final AppCartService appCartService;

    public AppCartController(AppCartService appCartService) {
        this.appCartService = appCartService;
    }

    @GetMapping
    public Result<AppCartResponse> detail(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(appCartService.getCurrentUserCart(loginUser.getUserId()));
    }

    @PostMapping("/item")
    public Result<Void> addItem(@Valid @RequestBody AppCartAddItemRequest request,
                                Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        appCartService.addItem(loginUser.getUserId(), request);
        return Result.success();
    }

    @PutMapping("/item/{foodItemId}")
    public Result<Void> updateQuantity(@PathVariable Long foodItemId,
                                       @Valid @RequestBody AppCartUpdateItemRequest request,
                                       Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        appCartService.updateQuantity(loginUser.getUserId(), foodItemId, request);
        return Result.success();
    }

    @DeleteMapping("/item/{foodItemId}")
    public Result<Void> removeItem(@PathVariable Long foodItemId,
                                   Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        appCartService.removeItem(loginUser.getUserId(), foodItemId);
        return Result.success();
    }

    @DeleteMapping("/clear")
    public Result<Void> clear(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        appCartService.clearCart(loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/checkout")
    public Result<AppCartCheckoutResponse> checkout(@RequestBody(required = false) AppCartCheckoutRequest request,
                                                    Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(appCartService.checkout(loginUser.getUserId(), request));
    }
}

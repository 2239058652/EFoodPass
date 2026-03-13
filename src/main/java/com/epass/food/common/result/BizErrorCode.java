package com.epass.food.common.result;

public final class BizErrorCode {

    /**
     * auth/system: 4001 - 4099
     */
    public static final int AUTH_USERNAME_OR_PASSWORD_INVALID = 4001;
    public static final int AUTH_USER_DISABLED = 4002;
    public static final int USER_NOT_FOUND = 4004;
    public static final int USER_ROLE_NOT_FOUND_OR_DISABLED = 4005;
    public static final int ROLE_CODE_EXISTS = 4006;
    public static final int ROLE_NOT_FOUND = 4007;
    public static final int PERMISSION_NOT_FOUND = 4008;
    public static final int PERMISSION_CODE_EXISTS = 4009;

    /**
     * system_user/system_role/system_permission: 4010 - 4099
     */
    public static final int USER_STATUS_INVALID = 4010;
    public static final int ADMIN_USER_CANNOT_DISABLE = 4011;
    public static final int ROLE_STATUS_INVALID = 4012;
    public static final int ADMIN_ROLE_CANNOT_DISABLE = 4013;
    public static final int PERMISSION_STATUS_INVALID = 4014;
    public static final int CORE_PERMISSION_CANNOT_DISABLE = 4015;
    public static final int ADMIN_USER_CANNOT_DELETE = 4016;
    public static final int ADMIN_ROLE_CANNOT_DELETE = 4017;
    public static final int CORE_PERMISSION_CANNOT_DELETE = 4018;
    public static final int PERMISSION_TYPE_INVALID = 4019;
    public static final int USERNAME_EXISTS = 4020;

    /**
     * food_category: 4101 - 4199
     */
    public static final int CATEGORY_STATUS_INVALID = 4101;
    public static final int CATEGORY_NAME_EXISTS = 4102;
    public static final int CATEGORY_NOT_FOUND = 4103;
    public static final int CATEGORY_HAS_ITEMS = 4104;

    /**
     * food_item: 4201 - 4299
     */
    public static final int ITEM_ON_SALE_STATUS_INVALID = 4201;
    public static final int ITEM_PRICE_INVALID = 4202;
    public static final int ITEM_STOCK_INVALID = 4203;
    public static final int ITEM_CATEGORY_NOT_FOUND = 4204;
    public static final int ITEM_CATEGORY_DISABLED = 4205;
    public static final int ITEM_NOT_FOUND = 4206;
    public static final int ITEM_NAME_EXISTS = 4207;
    public static final int ITEM_NAME_BLANK = 4208;
    public static final int ITEM_CATEGORY_DISABLED_FOR_ON_SALE = 4209;
    public static final int ITEM_HAS_ORDER_RELATION = 4210;

    /**
     * food_order: 4301 - 4399
     */
    public static final int ORDER_STATUS_INVALID = 4301;
    public static final int ORDER_NOT_FOUND = 4302;
    public static final int ORDER_USER_NOT_FOUND = 4303;
    public static final int ORDER_ITEMS_EMPTY = 4304;
    public static final int ORDER_ITEM_NOT_FOUND = 4305;
    public static final int ORDER_ITEM_NOT_ON_SALE = 4306;
    public static final int ORDER_ITEM_CATEGORY_INVALID = 4307;
    public static final int ORDER_COMPLETED_CANNOT_CANCEL = 4308;
    public static final int ORDER_ALREADY_CANCELED = 4309;
    public static final int ORDER_ONLY_PROCESSING_CAN_COMPLETE = 4310;
    public static final int ORDER_ONLY_PENDING_CAN_PROCESS = 4311;
    public static final int ORDER_USER_DISABLED = 4312;
    public static final int ORDER_ITEM_STOCK_NOT_ENOUGH = 4313;
    public static final int ORDER_RESTORE_ITEM_NOT_FOUND = 4314;
    public static final int ORDER_NO_PERMISSION = 4315;

    /**
     * food_stock_log: 4401 - 4499
     */
    public static final int STOCK_LOG_CHANGE_TYPE_INVALID = 4401;

    private BizErrorCode() {
    }
}

package com.project.DuAnTotNghiep.entity.enumClass;

public enum BillStatus {
    CHO_XAC_NHAN("Chờ xác nhận"),
    CHO_LAY_HANG("Chờ lấy hàng"), 
    CHO_GIAO_HANG("Chờ giao hàng"),
    HOAN_THANH("Hoàn thành"),
    HUY("Hủy"),
    TRA_HANG("Trả hàng");

    private final String displayName;

    BillStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}

package com.bootapp.core.utils;

import com.bootapp.core.grpc.CoreCommon;

public class CommonUtils {
    static public final double epsilon = 1e-10;
    public static Long safeValue(Long value) {
        if (value == null) return 0L;
        return value;
    }
    public static Integer safeValue(Integer value) {
        if (value == null) return 0;
        return value;
    }
    public static CoreCommon.Pagination.Builder buildPagination(long idx, long size, long total) {
        CoreCommon.Pagination.Builder paginationResp = CoreCommon.Pagination.newBuilder();
        paginationResp.setIdx(idx);
        paginationResp.setSize(size);
        paginationResp.setTotal(total);
        return paginationResp;
    }
}

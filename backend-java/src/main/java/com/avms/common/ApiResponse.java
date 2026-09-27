package com.avms.common;

/** DRF-compatible envelope: success -> {success:true,data,message}. */
public record ApiResponse<T>(boolean success, T data, String message) {
  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(true, data, message);
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null);
  }
}

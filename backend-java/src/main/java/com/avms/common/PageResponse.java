package com.avms.common;

import java.util.List;

/** DRF PageNumber-compatible page shape: {count,next,previous,results}. */
public record PageResponse<T>(long count, String next, String previous, List<T> results) {
  public static <T> PageResponse<T> of(long count, List<T> results) {
    return new PageResponse<>(count, null, null, results);
  }
}

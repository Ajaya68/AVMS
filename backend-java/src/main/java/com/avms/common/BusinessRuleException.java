package com.avms.common;

/** Business-rule violation -> HTTP 422 (mirrors Django 422 rejects: oversell/overpay/over-return). */
public class BusinessRuleException extends RuntimeException {
  public BusinessRuleException(String message) {
    super(message);
  }
}

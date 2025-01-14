package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.DELEGATE_TASK_EXPIRED;

import io.harness.eraro.Level;
import io.harness.exception.FailureType;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class DelegateTaskExpiredException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public DelegateTaskExpiredException(String taskId) {
    super(taskId, null, DELEGATE_TASK_EXPIRED, Level.ERROR, null, EnumSet.of(FailureType.EXPIRED));
    param(MESSAGE_KEY, taskId);
  }
}
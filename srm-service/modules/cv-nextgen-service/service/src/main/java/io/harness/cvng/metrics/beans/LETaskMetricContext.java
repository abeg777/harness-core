/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.metrics.beans;

import io.harness.metrics.AutoMetricContext;

import java.time.Duration;

public class LETaskMetricContext extends AutoMetricContext {
  public LETaskMetricContext(String accountId, String leTaskType, String status, Duration analysisDuration) {
    put("accountId", accountId);
    put("leTaskType", leTaskType);
    put("leTaskStatus", status);
    put("analysisDurationMinutes", Long.toString(analysisDuration.toMinutes()));
  }
}
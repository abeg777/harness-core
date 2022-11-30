/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.monitoredService;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CV)
@Value
public class MonitoredServiceResponse {
  @NotNull @JsonProperty("monitoredService") private MonitoredServiceDTO monitoredServiceDTO;
  private Long createdAt;
  private Long lastModifiedAt;
  @Builder
  public MonitoredServiceResponse(MonitoredServiceDTO monitoredService, Long createdAt, Long lastModifiedAt) {
    this.monitoredServiceDTO = monitoredService;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}
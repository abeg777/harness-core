/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.tas.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.CDP)
public interface TasResourceService {
  List<String> listOrganizationsForTas(
      String connectorRef, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  List<String> listSpacesForTas(String connectorRef, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String organization);
}

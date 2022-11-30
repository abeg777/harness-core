/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.core.beans.TimeSeriesSampleDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;

public interface SplunkService extends DataSourceConnectivityChecker {
  List<SplunkSavedSearch> getSavedSearches(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String requestGuid);

  List<LinkedHashMap> getSampleData(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String query, String requestGuid);
  SortedSet<TimeSeriesSampleDTO> getMetricSampleData(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String query, String requestGuid);

  List<LinkedHashMap> getLatestHistogram(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String query, String requestGuid);
}
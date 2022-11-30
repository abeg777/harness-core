/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.jobs;

import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;

public class MonitoredServiceNotificationHandler implements Handler<MonitoredService> {
  @Inject MonitoredServiceService monitoredServiceService;

  @Override
  public void handle(MonitoredService entity) {
    monitoredServiceService.handleNotification(entity);
  }
}
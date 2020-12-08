package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.NodeExecutionEventData;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.tasks.ResponseData;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ResumeNodeExecutionEventData implements NodeExecutionEventData {
  // TODO: move to sdk-commons
  List<PlanNodeProto> nodes;
  Map<String, ResponseData> response;
  boolean asyncError;
}

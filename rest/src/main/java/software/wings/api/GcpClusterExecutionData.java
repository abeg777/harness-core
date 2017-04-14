package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by brett on 4/13/17
 */
public class GcpClusterExecutionData extends StateExecutionData implements NotifyResponseData {
  private String clusterName;
  private String zone;
  private int nodeCount;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  public int getNodeCount() {
    return nodeCount;
  }

  public void setNodeCount(int nodeCount) {
    this.nodeCount = nodeCount;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "clusterName",
        anExecutionDataValue().withValue(clusterName).withDisplayName("Cluster Name").build());
    putNotNull(executionDetails, "zone", anExecutionDataValue().withValue(zone).withDisplayName("Zone").build());
    putNotNull(executionDetails, "nodeCount",
        anExecutionDataValue().withValue(nodeCount).withDisplayName("Node Count").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "clusterName",
        anExecutionDataValue().withValue(clusterName).withDisplayName("Cluster Name").build());
    putNotNull(executionDetails, "zone", anExecutionDataValue().withValue(zone).withDisplayName("Zone").build());
    putNotNull(executionDetails, "nodeCount",
        anExecutionDataValue().withValue(nodeCount).withDisplayName("Node Count").build());
    return executionDetails;
  }

  public static final class GcpClusterExecutionDataBuilder {
    private String clusterName;
    private String zone;
    private int nodeCount;

    private GcpClusterExecutionDataBuilder() {}

    public static GcpClusterExecutionDataBuilder aGcpClusterExecutionData() {
      return new GcpClusterExecutionDataBuilder();
    }

    public GcpClusterExecutionDataBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public GcpClusterExecutionDataBuilder withZone(String zone) {
      this.zone = zone;
      return this;
    }

    public GcpClusterExecutionDataBuilder withNodeCount(int nodeCount) {
      this.nodeCount = nodeCount;
      return this;
    }

    public GcpClusterExecutionDataBuilder but() {
      return aGcpClusterExecutionData().withClusterName(clusterName).withZone(zone).withNodeCount(nodeCount);
    }

    public GcpClusterExecutionData build() {
      GcpClusterExecutionData gcpClusterExecutionData = new GcpClusterExecutionData();
      gcpClusterExecutionData.setClusterName(clusterName);
      gcpClusterExecutionData.setZone(zone);
      gcpClusterExecutionData.setNodeCount(nodeCount);
      return gcpClusterExecutionData;
    }
  }
}

package org.apache.helix.controller.rebalancer.waged;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomUtils;
import org.apache.helix.controller.dataproviders.ManagementControllerDataProvider;
import org.apache.helix.controller.dataproviders.ResourceControllerDataProvider;
import org.apache.helix.controller.rebalancer.DelayedAutoRebalancer;
import org.apache.helix.controller.stages.AttributeName;
import org.apache.helix.controller.stages.ClusterEvent;
import org.apache.helix.controller.stages.ClusterEventType;
import org.apache.helix.controller.stages.CurrentStateOutput;
import org.apache.helix.model.ClusterConfig;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.model.Resource;
import org.apache.helix.model.ResourceConfig;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class TestWagedInstanceCapacityManager {

  private static final int INSTANCE_COUNT = 3;
  private static final int RESOURCE_COUNT = 2;
  private static final int PARTITION_COUNT = 3;
  private static final List<String> CAPACITY_KEYS = Lists.newArrayList("CU", "PARTCOUNT", "DISK");
  private static final Map<String, Integer> DEFAULT_INSTANCE_CAPACITY_MAP =
      ImmutableMap.of("CU", 100, "PARTCOUNT", 10, "DISK", 100);

  private static final Map<String, Integer> DEFAULT_PART_CAPACITY_MAP =
      ImmutableMap.of("CU", 10, "PARTCOUNT", 1, "DISK", 1);

  private ResourceControllerDataProvider _clusterData;
  private Map<String, Resource> _resourceMap;
  private CurrentStateOutput _currentStateOutput;
  private WagedInstanceCapacity _wagedInstanceCapacity;

  @BeforeMethod
  public void setUp() {
    // prepare cluster data
    _clusterData = Mockito.spy(new ResourceControllerDataProvider());
    Map<String, InstanceConfig> instanceConfigMap = generateInstanceCapacityConfigs();
    _clusterData.setInstanceConfigMap(instanceConfigMap);
    _clusterData.setResourceConfigMap(generateResourcePartitionCapacityConfigs());
    _clusterData.setIdealStates(generateIdealStates());
    Mockito.doReturn(ImmutableMap.of()).when(_clusterData).getAllInstancesMessages();

    ClusterConfig clusterConfig = new ClusterConfig("test");
    clusterConfig.setTopologyAwareEnabled(false);
    clusterConfig.setInstanceCapacityKeys(CAPACITY_KEYS);
    _clusterData.setClusterConfig(clusterConfig);

    // prepare current state output
    _resourceMap = generateResourceMap();
    _currentStateOutput = populateCurrentStatesForResources(_resourceMap, instanceConfigMap.keySet());

    // prepare instance of waged-instance capacity
    _wagedInstanceCapacity = new WagedInstanceCapacity(_clusterData);
  }

  @Test
  public void testProcessEventWithNoWagedResources() {
    // We create ideal states with all WAGED enabled.
    Map<String, IdealState> idealStates = _clusterData.getIdealStates();

    // remove one WAGED resource from all resources.
    idealStates.forEach((resourceName, idealState) -> {
      idealState.setRebalanceMode(IdealState.RebalanceMode.CUSTOMIZED);
      idealState.setRebalancerClassName(DelayedAutoRebalancer.class.getName());
    });

    ClusterEvent clusterEvent = new ClusterEvent("test", ClusterEventType.CurrentStateChange);
    clusterEvent.addAttribute(AttributeName.ControllerDataProvider.name(), _clusterData);
    clusterEvent.addAttribute(AttributeName.RESOURCES.name(), _resourceMap);

    WagedInstanceCapacityManager.getInstance()
        .processEvent(clusterEvent, _currentStateOutput);

    // validate that we did not compute and set the capacity map.
    Assert.assertNull(_clusterData.getWagedInstanceCapacity());
  }

  @Test
  public void testProcessEventWithSomeWagedResources() {
    // We create ideal states with all WAGED enabled.
    Map<String, IdealState> idealStates = _clusterData.getIdealStates();

    // remove WAGED from one resource.
    IdealState firstIdealState = idealStates.entrySet().iterator().next().getValue();
    firstIdealState.setRebalanceMode(IdealState.RebalanceMode.CUSTOMIZED);
    firstIdealState.setRebalancerClassName(DelayedAutoRebalancer.class.getName());
    int totalIdealStates = idealStates.size();

    ClusterEvent clusterEvent = new ClusterEvent("test", ClusterEventType.CurrentStateChange);
    clusterEvent.addAttribute(AttributeName.ControllerDataProvider.name(), _clusterData);
    clusterEvent.addAttribute(AttributeName.RESOURCES.name(), _resourceMap);

    WagedInstanceCapacityManager.getInstance()
        .processEvent(clusterEvent, _currentStateOutput);

    // validate that we did not compute and set the capacity map.
    WagedInstanceCapacity wagedInstanceCapacity = _clusterData.getWagedInstanceCapacity();
    Assert.assertNotNull(wagedInstanceCapacity);

    Map<String, Map<String, Set<String>>> allocatedPartitionsMap = wagedInstanceCapacity.getAllocatedPartitionsMap();
    Set<String> resourcesAllocated = allocatedPartitionsMap.values().stream()
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

    Assert.assertEquals(resourcesAllocated.size(), totalIdealStates - 1);
  }

  @Test
  public void testProcessEventWithAllWagedResources() {
    // We create ideal states with all WAGED enabled.
    Map<String, IdealState> idealStates = _clusterData.getIdealStates();

    ClusterEvent clusterEvent = new ClusterEvent("test", ClusterEventType.CurrentStateChange);
    clusterEvent.addAttribute(AttributeName.ControllerDataProvider.name(), _clusterData);
    clusterEvent.addAttribute(AttributeName.RESOURCES.name(), _resourceMap);

    WagedInstanceCapacityManager.getInstance()
        .processEvent(clusterEvent, _currentStateOutput);

    // validate that we did not compute and set the capacity map.
    WagedInstanceCapacity wagedInstanceCapacity = _clusterData.getWagedInstanceCapacity();
    Assert.assertNotNull(wagedInstanceCapacity);

    Map<String, Map<String, Set<String>>> allocatedPartitionsMap = wagedInstanceCapacity.getAllocatedPartitionsMap();
    Set<String> resourcesAllocated = allocatedPartitionsMap.values().stream()
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

    Assert.assertEquals(resourcesAllocated.size(), idealStates.size());
  }

  @Test
  public void testShouldNoOp() {
    // case: when cache is not of type: ResourceControllerDataProvider
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        new ManagementControllerDataProvider("a", "b"),
        ImmutableMap.of(),
        new ClusterEvent(ClusterEventType.LiveInstanceChange)));

    // case: when resource-map is null
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        new ResourceControllerDataProvider(), null, new ClusterEvent(ClusterEventType.LiveInstanceChange)));

    // case: when resource-map is empty
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        new ResourceControllerDataProvider(), ImmutableMap.of(), new ClusterEvent(ClusterEventType.LiveInstanceChange)));

    // case: when instance capacity is null
    Assert.assertFalse(WagedInstanceCapacityManager.shouldNoOp(
        new ResourceControllerDataProvider(), _resourceMap, new ClusterEvent(ClusterEventType.LiveInstanceChange)));

    // case: when event is of no-op
    ResourceControllerDataProvider dataProvider = Mockito.mock(ResourceControllerDataProvider.class);
    WagedInstanceCapacity instanceCapacity = Mockito.mock(WagedInstanceCapacity.class);
    Mockito.when(dataProvider.getWagedInstanceCapacity()).thenReturn(instanceCapacity);

    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.CustomizedStateChange)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.CustomizedViewChange)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.CustomizeStateConfigChange)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.ExternalViewChange)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.IdealStateChange)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.OnDemandRebalance)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.Resume)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.RetryRebalance)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.StateVerifier)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.TargetExternalViewChange)));
    Assert.assertTrue(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.TaskCurrentStateChange)));

    Assert.assertFalse(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.LiveInstanceChange)));
    Assert.assertFalse(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.CurrentStateChange)));
    Assert.assertFalse(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.MessageChange)));
    Assert.assertFalse(WagedInstanceCapacityManager.shouldNoOp(
        dataProvider, _resourceMap, new ClusterEvent(ClusterEventType.PeriodicalRebalance)));
  }

  // -- static helpers
  private Map<String, InstanceConfig> generateInstanceCapacityConfigs() {
    Map<String, InstanceConfig> instanceConfigMap = new HashMap<>();

    for (int i = 0; i < INSTANCE_COUNT; i ++) {
      String instanceName = "instance-" + i;
      InstanceConfig config = new InstanceConfig(instanceName);
      config.setInstanceCapacityMap(DEFAULT_INSTANCE_CAPACITY_MAP);
      instanceConfigMap.put(instanceName, config);
    }

    return instanceConfigMap;
  }

  private Map<String, ResourceConfig> generateResourcePartitionCapacityConfigs() {
    Map<String, ResourceConfig> resourceConfigMap = new HashMap<>();

    try {
      Map<String, Map<String, Integer>> partitionsCapacityMap = new HashMap<>();
      partitionsCapacityMap.put("DEFAULT", DEFAULT_PART_CAPACITY_MAP);

      for (String resourceName : getResourceNames()) {
        ResourceConfig config = new ResourceConfig(resourceName);
        config.setPartitionCapacityMap(partitionsCapacityMap);
        resourceConfigMap.put(resourceName, config);
      }
    } catch(IOException e) {
      throw new RuntimeException("error while setting partition capacity map");
    }
    return resourceConfigMap;
  }

  private List<IdealState> generateIdealStates() {
    return getResourceNames().stream()
        .map(resourceName -> {
          IdealState idealState = new IdealState(resourceName);
          idealState.setRebalanceMode(IdealState.RebalanceMode.FULL_AUTO);
          idealState.setRebalancerClassName(WagedRebalancer.class.getName());
          return idealState;
        })
        .collect(Collectors.toList());
  }

  private static CurrentStateOutput populateCurrentStatesForResources(
      Map<String, Resource> resourceMap, Set<String> instanceNames) {
    CurrentStateOutput currentStateOutput = new CurrentStateOutput();

    resourceMap.forEach((resourceName, resource) ->
        resource.getPartitions().forEach(partition -> {
          int masterPartIdx = RandomUtils.nextInt(0, instanceNames.size());
          int idx = 0;
          for (Iterator<String> it = instanceNames.iterator(); it.hasNext(); idx ++) {
            currentStateOutput.setCurrentState(
                resourceName, partition, it.next(), (idx == masterPartIdx) ? "MASTER" : "SLAVE");
          }
        }));

    return currentStateOutput;
  }

  private static Map<String, Resource> generateResourceMap() {
    return getResourceNames().stream()
        .map(resourceName -> {
          Resource resource = new Resource(resourceName);
          IntStream.range(0, PARTITION_COUNT)
              .mapToObj(i -> "partition-" + i)
              .forEach(resource::addPartition);
          return resource;
        })
        .collect(Collectors.toMap(Resource::getResourceName, Function.identity()));
  }

  private static List<String> getResourceNames() {
    return IntStream.range(0, RESOURCE_COUNT)
        .mapToObj(i -> "resource-" + i)
        .collect(Collectors.toList());
  }

}

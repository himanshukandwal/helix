package org.apache.helix.common;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.apache.commons.lang3.RandomUtils;
import org.apache.helix.BaseDataAccessor;
import org.apache.helix.SystemPropertyKeys;
import org.apache.helix.TestHelper;
import org.apache.helix.manager.zk.ZkBaseDataAccessor;
import org.apache.helix.tools.ClusterSetup;
import org.apache.helix.util.ZKClientPool;
import org.apache.helix.zookeeper.api.client.HelixZkClient;
import org.apache.helix.zookeeper.datamodel.ZNRecord;
import org.apache.helix.zookeeper.datamodel.serializer.ZNRecordSerializer;
import org.apache.helix.zookeeper.impl.factory.DedicatedZkClientFactory;
import org.apache.helix.zookeeper.zkclient.ZkServer;

/**
 * A placeholder class for test execution runtime variables.
 */
public class IntegrationTestRuntime {

  private static final int NUMBER_OF_ZK_SERVERS = 3;
  private static final int ZK_START_PORT = 2183;
  private static final String MULTI_ZK_PROPERTY_KEY = "multiZk";
  private static final String NUM_ZK_PROPERTY_KEY = "numZk";
  private static final String ZK_PREFIX = "localhost:";

  /*
   * Multiple ZK references
   */
  // The following maps hold ZK connect string as keys
  protected static final Map<String, ZkServer> _zkServerMap = new HashMap<>();
  protected static final Map<String, HelixZkClient> _helixZkClientMap = new HashMap<>();
  protected static final Map<String, ClusterSetup> _clusterSetupMap = new HashMap<>();
  protected static final Map<String, BaseDataAccessor> _baseDataAccessorMap = new HashMap<>();

  protected static final MBeanServerConnection _server = ManagementFactory.getPlatformMBeanServer();

  private final String _zkAddr;
  private final ZkServer _zkServer;
  private final HelixZkClient _gZkClient;
  private final ClusterSetup _gSetupTool;
  private final BaseDataAccessor<ZNRecord> _baseAccessor;

  private IntegrationTestRuntime(String zkAddr, ZkServer zkServer, HelixZkClient gZkClient, ClusterSetup gSetupTool,
      BaseDataAccessor<ZNRecord> baseAccessor) {
    _zkAddr = zkAddr;
    _zkServer = zkServer;
    _gZkClient = gZkClient;
    _gSetupTool = gSetupTool;
    _baseAccessor = baseAccessor;
  }

  public static IntegrationTestRuntime getRuntime() {
    int index = RandomUtils.nextInt(0, _zkServerMap.size());
    String zkAddr = _zkServerMap.keySet().stream().skip(index).findFirst().get();

    return new IntegrationTestRuntime(
        zkAddr, _zkServerMap.get(zkAddr), _helixZkClientMap.get(zkAddr),
        _clusterSetupMap.get(zkAddr), _baseDataAccessorMap.get(zkAddr));
  }

  public String getZkAddr() {
    return _zkAddr;
  }

  public ZkServer getZkServer() {
    return _zkServer;
  }

  public HelixZkClient getZkClient() {
    return _gZkClient;
  }

  public ClusterSetup getSetupTool() {
    return _gSetupTool;
  }

  public BaseDataAccessor<ZNRecord> getBaseAccessor() {
    return _baseAccessor;
  }

  public MBeanServerConnection getServer() {
    return _server;
  }

  protected static void initialize() {
    // TODO: use logging.properties file to config java.util.logging.Logger levels
    java.util.logging.Logger topJavaLogger = java.util.logging.Logger.getLogger("");
    topJavaLogger.setLevel(Level.WARNING);

    // Due to ZOOKEEPER-2693 fix, we need to specify whitelist for execute zk commends
    System.setProperty("zookeeper.4lw.commands.whitelist", "*");
    System.setProperty(SystemPropertyKeys.CONTROLLER_MESSAGE_PURGE_DELAY, "3000");

    // Start in-memory ZooKeepers. If multi-ZooKeeper is enabled, start more ZKs. Otherwise, just set up one ZK
    AtomicReference<Integer> numZkToStart = new AtomicReference<>(NUMBER_OF_ZK_SERVERS);
    Optional.ofNullable(System.getProperty(MULTI_ZK_PROPERTY_KEY))
        .map(Boolean.TRUE.toString()::equalsIgnoreCase)
        .ifPresent(b -> {
          Integer numZKFromConfig = Optional.ofNullable(System.getProperty(NUM_ZK_PROPERTY_KEY))
              .map(Integer::parseInt)
              .orElseThrow(() -> new RuntimeException("multiZk config is set but numZk config is missing!"));

          numZkToStart.set(Math.max(numZkToStart.get(), numZKFromConfig));
        });

    // Start "numZkFromConfigInt" ZooKeepers
    for (int i = 0; i < numZkToStart.get(); i++) {
      startZooKeeper(i);
    }

    cleanupJMXObjects();
  }

  /**
   * Unwinds the Test Execution Runtime.
   */
  public void unwind() {
    cleanupJMXObjects();
    synchronized (IntegrationTestRuntime.class) {
      // Close all ZK resources
      _baseDataAccessorMap.values().forEach(BaseDataAccessor::close);
      _clusterSetupMap.values().forEach(ClusterSetup::close);
      _helixZkClientMap.values().forEach(HelixZkClient::close);
      _zkServerMap.values().forEach(TestHelper::stopZkServer);
      ZKClientPool.reset();
    }
  }

  /**
   * Clean up all JMX beans from the MBean Server.
   */
  public static void cleanupJMXObjects() {
    try {
      // Clean up all JMX objects
      for (ObjectName mbean : _server.queryNames(null, null)) {
        _server.unregisterMBean(mbean);
      }
    } catch (Exception e) {
      // OK
    }
  }

  /**
   * Starts an additional in-memory ZooKeeper for testing.
   * @param i index to be added to the ZK port to avoid conflicts
   * @throws Exception
   */
  private static synchronized void startZooKeeper(int i) {
    String zkAddress = ZK_PREFIX + (ZK_START_PORT + i);
    _zkServerMap.computeIfAbsent(zkAddress, TestHelper::startZkServer);
    _helixZkClientMap.computeIfAbsent(zkAddress, IntegrationTestRuntime::createZkClient);
    _clusterSetupMap.computeIfAbsent(zkAddress, key -> new ClusterSetup(_helixZkClientMap.get(key)));
    _baseDataAccessorMap.computeIfAbsent(zkAddress, key -> new ZkBaseDataAccessor(_helixZkClientMap.get(key)));
  }

  private static HelixZkClient createZkClient(String zkAddress) {
    HelixZkClient.ZkClientConfig clientConfig = new HelixZkClient.ZkClientConfig()
        .setZkSerializer(new ZNRecordSerializer())
        .setConnectInitTimeout(1000L)
        .setOperationRetryTimeout(1000L);

    HelixZkClient.ZkConnectionConfig zkConnectionConfig = new HelixZkClient.ZkConnectionConfig(zkAddress)
        .setSessionTimeout(1000);

    return DedicatedZkClientFactory.getInstance().buildZkClient(zkConnectionConfig, clientConfig);
  }

}

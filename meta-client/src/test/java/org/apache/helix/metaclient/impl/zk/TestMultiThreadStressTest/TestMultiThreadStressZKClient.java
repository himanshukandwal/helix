package org.apache.helix.metaclient.impl.zk.TestMultiThreadStressTest;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.helix.metaclient.api.ChildChangeListener;
import org.apache.helix.metaclient.impl.zk.ZkMetaClient;
import org.apache.helix.metaclient.impl.zk.ZkMetaClientTestBase;
import org.apache.helix.metaclient.puppy.ExecDelay;
import org.apache.helix.metaclient.puppy.PuppyManager;
import org.apache.helix.metaclient.puppy.PuppyMode;
import org.apache.helix.metaclient.puppy.PuppySpec;
import org.apache.helix.metaclient.puppy.AbstractPuppy;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMultiThreadStressZKClient extends ZkMetaClientTestBase {

  private ZkMetaClient<String> _zkMetaClient;
  private final String zkParentKey = "/test";

  private final long TIMEOUT = 10; // The desired timeout duration of tests in seconds
  private final long EXEC_DELAY_DURATION_MILLIS = 500;

  @BeforeTest
  private void setUp() {
    this._zkMetaClient = createZkMetaClient();
    this._zkMetaClient.connect();
  }

  @Test
  public void testCreatePuppy() {
    _zkMetaClient.create(zkParentKey, "test");

    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 5);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);
    CreatePuppy createPuppy2 = new CreatePuppy(_zkMetaClient, puppySpec);
    CreatePuppy createPuppy3 = new CreatePuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);
    puppyManager.addPuppy(createPuppy2);
    puppyManager.addPuppy(createPuppy3);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, null);

    // cleanup
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
  }

  @Test
  public void testDeletePuppy() {
    _zkMetaClient.create(zkParentKey, "test");

    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 5);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);
    DeletePuppy deletePuppy = new DeletePuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);
    puppyManager.addPuppy(deletePuppy);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, null);

    // cleanup
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
  }

  @Test
  public void testGetPuppy() {
    _zkMetaClient.create(zkParentKey, "test");

    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 5);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);
    GetPuppy getPuppy = new GetPuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);
    puppyManager.addPuppy(getPuppy);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, null);

    // cleanup
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
  }

  @Test
  public void testSetPuppy() {
    _zkMetaClient.create(zkParentKey, "test");

    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 5);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);
    SetPuppy setPuppy = new SetPuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);
    puppyManager.addPuppy(setPuppy);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, null);

    // cleanup
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
  }

  @Test
  public void testUpdatePuppy() {
    _zkMetaClient.create(zkParentKey, "test");

    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 5);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);
    UpdatePuppy updatePuppy = new UpdatePuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);
    puppyManager.addPuppy(updatePuppy);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, null);

    // cleanup
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
  }

  @Test
  public void testCrudPuppies() {
    _zkMetaClient.create(zkParentKey, "test");

    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 5);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);
    GetPuppy getPuppy = new GetPuppy(_zkMetaClient, puppySpec);
    DeletePuppy deletePuppy = new DeletePuppy(_zkMetaClient, puppySpec);
    SetPuppy setPuppy = new SetPuppy(_zkMetaClient, puppySpec);
    UpdatePuppy updatePuppy = new UpdatePuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);
    puppyManager.addPuppy(getPuppy);
    puppyManager.addPuppy(deletePuppy);
    puppyManager.addPuppy(setPuppy);
    puppyManager.addPuppy(updatePuppy);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, null);

    // cleanup
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
  }


  @Test
  public void testBasicParentListenerPuppy() {
    _zkMetaClient.create(zkParentKey, "test");
    AtomicInteger globalChildChangeCounter = new AtomicInteger();
    ChildChangeListener childChangeListener = (changedPath, changeType) -> {
      globalChildChangeCounter.addAndGet(1);
      System.out.println("-------------- Child change detected: " + changeType + " at path: " + changedPath + ". Number of total changes: " + globalChildChangeCounter.get());
    };

    _zkMetaClient.subscribeChildChanges(zkParentKey, childChangeListener, false);

    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 5);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, globalChildChangeCounter);

    // cleanup
    _zkMetaClient.unsubscribeChildChanges(zkParentKey, childChangeListener);
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
  }

  @Test
  public void testComplexParentListenerPuppy() {
    _zkMetaClient.create(zkParentKey, "test");
    // Global counter for all child changes
    AtomicInteger globalChildChangeCounter = new AtomicInteger();
    ChildChangeListener childChangeListener = (changedPath, changeType) -> {
      globalChildChangeCounter.addAndGet(1);
      System.out.println("-------------- Child change detected: " + changeType + " at path: " + changedPath + " number of changes: " + globalChildChangeCounter.get());
    };
    _zkMetaClient.subscribeChildChanges(zkParentKey, childChangeListener, false);

    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 5);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);
    GetPuppy getPuppy = new GetPuppy(_zkMetaClient, puppySpec);
    DeletePuppy deletePuppy = new DeletePuppy(_zkMetaClient, puppySpec);
    SetPuppy setPuppy = new SetPuppy(_zkMetaClient, puppySpec);
    UpdatePuppy updatePuppy = new UpdatePuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);
    puppyManager.addPuppy(getPuppy);
    puppyManager.addPuppy(deletePuppy);
    puppyManager.addPuppy(setPuppy);
    puppyManager.addPuppy(updatePuppy);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, globalChildChangeCounter);

    // cleanup
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
    _zkMetaClient.unsubscribeChildChanges(zkParentKey, childChangeListener);
    _zkMetaClient.delete(zkParentKey);
  }


  @Test
  public void testChildListenerPuppy() {
    _zkMetaClient.create(zkParentKey, "test");
    // Setting num diff paths to 3 until we find a better way of scaling listeners.
    PuppySpec puppySpec = new PuppySpec(PuppyMode.REPEAT, 0.2f, new ExecDelay(EXEC_DELAY_DURATION_MILLIS, 0.1f), 3);
    CreatePuppy createPuppy = new CreatePuppy(_zkMetaClient, puppySpec);
    GetPuppy getPuppy = new GetPuppy(_zkMetaClient, puppySpec);
    DeletePuppy deletePuppy = new DeletePuppy(_zkMetaClient, puppySpec);
    SetPuppy setPuppy = new SetPuppy(_zkMetaClient, puppySpec);
    UpdatePuppy updatePuppy = new UpdatePuppy(_zkMetaClient, puppySpec);

    PuppyManager puppyManager = new PuppyManager();
    puppyManager.addPuppy(createPuppy);
    puppyManager.addPuppy(getPuppy);
    puppyManager.addPuppy(deletePuppy);
    puppyManager.addPuppy(setPuppy);
    puppyManager.addPuppy(updatePuppy);

    // Create a child listener for each child defined in number diff paths in puppyspec.
    // TODO: Make this a parameter for a loop.
    AtomicInteger childChangeCounter0 = new AtomicInteger();
    ChildChangeListener childChangeListener0 = (changedPath, changeType) -> {
      childChangeCounter0.addAndGet(1);
      System.out.println("-------------- Child change detected: " + changeType + " at path: " + changedPath + " number of changes: " + childChangeCounter0.get());
    };
    _zkMetaClient.subscribeChildChanges("/test/0", childChangeListener0, false);

    AtomicInteger childChangeCounter1 = new AtomicInteger();
    ChildChangeListener childChangeListener1 = (changedPath, changeType) -> {
      childChangeCounter1.addAndGet(1);
      System.out.println("-------------- Child change detected: " + changeType + " at path: " + changedPath + " number of changes: " + childChangeCounter1.get());
    };
    _zkMetaClient.subscribeChildChanges("/test/1", childChangeListener1, false);

    AtomicInteger childChangeCounter2 = new AtomicInteger();
    ChildChangeListener childChangeListener2 = (changedPath, changeType) -> {
      childChangeCounter2.addAndGet(1);
      System.out.println("-------------- Child change detected: " + changeType + " at path: " + changedPath + " number of changes: " + childChangeCounter2.get());
    };
    _zkMetaClient.subscribeChildChanges("/test/2", childChangeListener2, false);

    puppyManager.start(TIMEOUT);

    assertNoExceptions(puppyManager, null);

    // Add all event changes from all puppies and compare with child change listener
    // Inner merged by path
    Map<String, Integer> mergedEventChangeCounterMap = new HashMap<>();
    for (AbstractPuppy puppy : puppyManager.getPuppies()) {
      puppy._eventChangeCounterMap.forEach((key, value) -> {
        if (mergedEventChangeCounterMap.containsKey(key)) {
          mergedEventChangeCounterMap.put(key, mergedEventChangeCounterMap.get(key) + value);
        } else {
          mergedEventChangeCounterMap.put(key, value);
        }
      });
    }

    System.out.println("Merged event change counter map: " + mergedEventChangeCounterMap);
    System.out.println("Child change counter 0: " + childChangeCounter0);
    System.out.println("Child change counter 1: " + childChangeCounter1);
    System.out.println("Child change counter 2: " + childChangeCounter2);
    Assert.assertEquals(childChangeCounter0.get(), mergedEventChangeCounterMap.getOrDefault("0", 0).intValue());
    Assert.assertEquals(childChangeCounter1.get(), mergedEventChangeCounterMap.getOrDefault("1", 0).intValue());
    Assert.assertEquals(childChangeCounter2.get(), mergedEventChangeCounterMap.getOrDefault("2", 0).intValue());

    // cleanup
    _zkMetaClient.unsubscribeChildChanges("/test/0", childChangeListener0);
    _zkMetaClient.unsubscribeChildChanges("/test/1", childChangeListener1);
    _zkMetaClient.unsubscribeChildChanges("/test/2", childChangeListener2);
    _zkMetaClient.recursiveDelete(zkParentKey);
    Assert.assertEquals(_zkMetaClient.countDirectChildren(zkParentKey), 0);
  }

  private void assertNoExceptions(PuppyManager puppyManager, AtomicInteger globalChangeCounter) {
    int totalUnhandledErrors = 0;
    int totalEventChanges = 0;

    // Add all change counters and compare with event change listener
    for (AbstractPuppy puppy : puppyManager.getPuppies()) {
      AtomicInteger totalHandledErrors = new AtomicInteger();
      puppy._eventChangeCounterMap.forEach((key, value) -> {
        totalHandledErrors.addAndGet(value);
      });

      System.out.println("Change counter: " + totalHandledErrors + " for " + puppy.getClass());
      System.out.println("Error counter: " + puppy._unhandledErrorCounter + " for " + puppy.getClass());
      totalUnhandledErrors += puppy._unhandledErrorCounter;
      totalEventChanges += totalHandledErrors.get();
    }

    // Assert no unhandled (unexpected) exceptions and that the child change listener placed on
    // test parent node (/test) caught all successful changes that were recorded by each puppy
    Assert.assertEquals(totalUnhandledErrors, 0);

    // Assert that the global change counter matches the total number of events recorded by each puppy
    if (globalChangeCounter != null) {
      Assert.assertEquals(totalEventChanges, globalChangeCounter.get());
    }
  }
}

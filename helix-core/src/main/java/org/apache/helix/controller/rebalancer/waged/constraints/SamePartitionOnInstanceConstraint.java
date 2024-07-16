package org.apache.helix.controller.rebalancer.waged.constraints;

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

import java.util.Set;
import org.apache.helix.controller.rebalancer.waged.model.AssignableNode;
import org.apache.helix.controller.rebalancer.waged.model.AssignableReplica;
import org.apache.helix.controller.rebalancer.waged.model.ClusterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class SamePartitionOnInstanceConstraint extends HardConstraint {

  private static final Logger LOG = LoggerFactory.getLogger(SamePartitionOnInstanceConstraint.class);

  @Override
  boolean isAssignmentValid(AssignableNode node, AssignableReplica replica,
      ClusterContext clusterContext) {
    Set<String> assignedPartitionsByResource = node.getAssignedPartitionsByResource(replica.getResourceName());

    if (assignedPartitionsByResource.contains(replica.getPartitionName())) {
      if (enableLogging) {
        LOG.info("Same partition ({}) of different states cannot co-exist in one instance", replica.getPartitionName());
      }
      return false;
    }
    return true;
  }

  @Override
  String getDescription() {
    return "Same partition of different states cannot co-exist in one instance";
  }
}

/*
 *    Copyright 2010-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.jpetstore.inventory.api;

public class InsufficientInventoryException extends RuntimeException {

  private static final long serialVersionUID = 1325893048140792295L;

  private final String itemId;
  private final int requestedQuantity;
  private final int availableQuantity;

  public InsufficientInventoryException(String itemId, int requestedQuantity, int availableQuantity) {
    super("Insufficient inventory for item " + itemId + ": requested " + requestedQuantity + ", available "
        + availableQuantity + ".");
    this.itemId = itemId;
    this.requestedQuantity = requestedQuantity;
    this.availableQuantity = availableQuantity;
  }

  public String getItemId() {
    return itemId;
  }

  public int getRequestedQuantity() {
    return requestedQuantity;
  }

  public int getAvailableQuantity() {
    return availableQuantity;
  }
}

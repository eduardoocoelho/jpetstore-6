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
package org.mybatis.jpetstore.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mybatis.jpetstore.inventory.api.InventoryQueryService;
import org.mybatis.jpetstore.inventory.api.InventoryReservationService;
import org.mybatis.jpetstore.inventory.api.InventoryStatus;
import org.mybatis.jpetstore.inventory.persistence.InventoryMapper;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

  @Mock
  private InventoryMapper inventoryMapper;

  @InjectMocks
  private InventoryService inventoryService;

  @Test
  void shouldImplementInventoryApis() {
    assertThat(inventoryService).isInstanceOf(InventoryQueryService.class)
        .isInstanceOf(InventoryReservationService.class);
  }

  @Test
  void shouldReturnQuantityFromMapper() {
    // given
    String itemId = "EST-1";
    when(inventoryMapper.getInventoryQuantity(itemId)).thenReturn(5);

    // when
    int quantity = inventoryService.getQuantity(itemId);

    // then
    assertThat(quantity).isEqualTo(5);
  }

  @Test
  void shouldReturnTrueWhenQuantityIsGreaterThanZero() {
    // given
    String itemId = "EST-1";
    when(inventoryMapper.getInventoryQuantity(itemId)).thenReturn(1);

    // when
    boolean inStock = inventoryService.isInStock(itemId);

    // then
    assertThat(inStock).isTrue();
  }

  @Test
  void shouldReturnFalseWhenQuantityIsZero() {
    // given
    String itemId = "EST-1";
    when(inventoryMapper.getInventoryQuantity(itemId)).thenReturn(0);

    // when
    boolean inStock = inventoryService.isInStock(itemId);

    // then
    assertThat(inStock).isFalse();
  }

  @Test
  void shouldMapInventoryStatus() {
    // given
    String itemId = "EST-1";
    when(inventoryMapper.getInventoryQuantity(itemId)).thenReturn(3);

    // when
    InventoryStatus inventoryStatus = inventoryService.getInventoryStatus(itemId);

    // then
    assertThat(inventoryStatus.itemId()).isEqualTo(itemId);
    assertThat(inventoryStatus.quantity()).isEqualTo(3);
    assertThat(inventoryStatus.inStock()).isTrue();
  }

  @Test
  void shouldDecrementInventoryThroughMapper() {
    // given
    String itemId = "EST-1";
    int quantity = 4;
    Map<String, Object> expectedParam = new HashMap<>(2);
    expectedParam.put("itemId", itemId);
    expectedParam.put("increment", quantity);

    // when
    inventoryService.decrement(itemId, quantity);

    // then
    verify(inventoryMapper).updateInventoryQuantity(eq(expectedParam));
  }

}

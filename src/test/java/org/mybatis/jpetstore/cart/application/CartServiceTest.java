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
package org.mybatis.jpetstore.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mybatis.jpetstore.catalog.api.CatalogQueryService;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.inventory.api.InventoryQueryService;

class CartServiceTest {

  private final CatalogQueryService catalogQueryService = mock(CatalogQueryService.class);
  private final InventoryQueryService inventoryQueryService = mock(InventoryQueryService.class);
  private final CartService cartService = new CartService(catalogQueryService, inventoryQueryService);

  @Test
  void shouldLoadItemAndStockWhenItemIsAbsent() {
    // given
    Cart cart = new Cart();
    Item item = item("EST-1");
    when(inventoryQueryService.isInStock("EST-1")).thenReturn(true);
    when(catalogQueryService.getItem("EST-1")).thenReturn(item);

    // when
    cartService.addItem(cart, "EST-1");

    // then
    assertThat(cart.getNumberOfItems()).isEqualTo(1);
    assertThat(cart.getCartItemList().get(0).getItem()).isSameAs(item);
    assertThat(cart.getCartItemList().get(0).isInStock()).isTrue();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(1);
  }

  @Test
  void shouldIncrementQuantityWithoutReloadingDetailsWhenItemIsPresent() {
    // given
    Cart cart = new Cart();
    cart.addItem(item("EST-1"), true);

    // when
    cartService.addItem(cart, "EST-1");

    // then
    assertThat(cart.getNumberOfItems()).isEqualTo(1);
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(2);
    verify(inventoryQueryService, never()).isInStock("EST-1");
    verify(catalogQueryService, never()).getItem("EST-1");
  }

  private static Item item(String itemId) {
    Item item = new Item();
    item.setItemId(itemId);
    item.setListPrice(new BigDecimal("16.50"));
    return item;
  }

}

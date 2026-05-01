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

import org.mybatis.jpetstore.cart.domain.Cart;
import org.mybatis.jpetstore.catalog.api.CatalogQueryService;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.inventory.api.InventoryQueryService;
import org.springframework.stereotype.Service;

@Service
public class CartService {

  private final CatalogQueryService catalogQueryService;
  private final InventoryQueryService inventoryQueryService;

  public CartService(CatalogQueryService catalogQueryService, InventoryQueryService inventoryQueryService) {
    this.catalogQueryService = catalogQueryService;
    this.inventoryQueryService = inventoryQueryService;
  }

  public void addItem(Cart cart, String itemId) {
    if (cart.containsItemId(itemId)) {
      cart.incrementQuantityByItemId(itemId);
    } else {
      boolean isInStock = inventoryQueryService.isInStock(itemId);
      ItemSnapshot item = catalogQueryService.getItemSnapshot(itemId);
      cart.addItem(item, isInStock);
    }
  }

}

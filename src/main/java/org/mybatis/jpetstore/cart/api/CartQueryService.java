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
package org.mybatis.jpetstore.cart.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.catalog.api.ProductSummary;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.CartItem;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.domain.Product;

public interface CartQueryService {

  Iterator<CartItem> getAllCartItems(Cart cart);

  int getNumberOfItems(Cart cart);

  BigDecimal getSubTotal(Cart cart);

  default CartSnapshot getCartSnapshot(Cart cart) {
    List<CartLineSnapshot> lines = new ArrayList<>();
    Iterator<CartItem> cartItems = cart.getAllCartItems();
    while (cartItems.hasNext()) {
      CartItem cartItem = cartItems.next();
      lines.add(toCartLineSnapshot(cartItem));
    }
    return new CartSnapshot(lines, cart.getSubTotal());
  }

  private static CartLineSnapshot toCartLineSnapshot(CartItem cartItem) {
    return new CartLineSnapshot(toItemSnapshot(cartItem.getItem()), cartItem.getQuantity(), cartItem.isInStock(),
        cartItem.getTotal());
  }

  private static ItemSnapshot toItemSnapshot(Item item) {
    Product product = item.getProduct();
    ProductSummary productSummary = null;
    if (product != null) {
      productSummary = new ProductSummary(product.getProductId(), product.getCategoryId(), product.getName(),
          product.getDescription());
    }
    return new ItemSnapshot(item.getItemId(), product == null ? null : product.getProductId(), productSummary,
        item.getListPrice(), item.getStatus(), item.getAttribute1(), item.getAttribute2(), item.getAttribute3(),
        item.getAttribute4(), item.getAttribute5());
  }

}

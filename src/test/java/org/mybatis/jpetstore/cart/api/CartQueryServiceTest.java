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

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.mybatis.jpetstore.catalog.domain.Item;
import org.mybatis.jpetstore.catalog.domain.Product;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.CartItem;

class CartQueryServiceTest {

  private final CartQueryService cartQueryService = new CartQueryService() {

    @Override
    public Iterator<CartItem> getAllCartItems(Cart cart) {
      return cart.getAllCartItems();
    }

    @Override
    public int getNumberOfItems(Cart cart) {
      return cart.getNumberOfItems();
    }

    @Override
    public BigDecimal getSubTotal(Cart cart) {
      return cart.getSubTotal();
    }
  };

  @Test
  void shouldMapCartToCartSnapshot() {
    // given
    Product product = new Product();
    product.setProductId("FI-SW-01");
    product.setCategoryId("FISH");
    product.setName("Angelfish");
    product.setDescription("Fresh Water fish from China");

    Item item = new Item();
    item.setItemId("EST-1");
    item.setProduct(product);
    item.setListPrice(new BigDecimal("16.50"));
    item.setStatus("P");
    item.setAttribute1("Large");

    Cart cart = new Cart();
    cart.addItem(item, true);
    cart.setQuantityByItemId("EST-1", 2);

    // when
    CartSnapshot cartSnapshot = cartQueryService.getCartSnapshot(cart);

    // then
    assertThat(cartSnapshot.numberOfItems()).isEqualTo(1);
    assertThat(cartSnapshot.subTotal()).isEqualTo(new BigDecimal("33.00"));
    assertThat(cartSnapshot.lines()).hasSize(1);
    assertThat(cartSnapshot.lines().get(0).quantity()).isEqualTo(2);
    assertThat(cartSnapshot.lines().get(0).inStock()).isTrue();
    assertThat(cartSnapshot.lines().get(0).total()).isEqualTo(new BigDecimal("33.00"));
    assertThat(cartSnapshot.lines().get(0).item().itemId()).isEqualTo("EST-1");
    assertThat(cartSnapshot.lines().get(0).item().product().productId()).isEqualTo("FI-SW-01");
    assertThat(cartSnapshot.lines().get(0).item().product().name()).isEqualTo("Angelfish");
  }

}

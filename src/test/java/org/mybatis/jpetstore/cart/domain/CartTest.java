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
package org.mybatis.jpetstore.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;

class CartTest {

  @Test
  void addItemWhenIsInStockIsTrue() {
    // given
    Cart cart = new Cart();
    ItemSnapshot item = item("I01", "2.05");

    // when
    cart.addItem(item, true);
    cart.addItem(item, true);

    // then
    assertThat(cart.getCartItemList().get(0).getItem()).isSameAs(item);
    assertThat(cart.getCartItemList().get(0).isInStock()).isTrue();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(2);
    assertThat(cart.getCartItemList().get(0).getTotal()).isEqualTo(new BigDecimal("4.10"));
    assertThat(cart.containsItemId("I01")).isTrue();
    assertThat(cart.getNumberOfItems()).isEqualTo(1);
    {
      Iterator<CartItem> cartItems = cart.getCartItems();
      assertThat(cartItems.next()).isNotNull();
      assertThat(cartItems.hasNext()).isFalse();
    }
    {
      Iterator<CartItem> cartItems = cart.getAllCartItems();
      assertThat(cartItems.next()).isNotNull();
      assertThat(cartItems.hasNext()).isFalse();
    }
  }

  @Test
  void addItemWhenIsInStockIsFalse() {
    // given
    Cart cart = new Cart();
    ItemSnapshot item = item("I01", "2.05");

    // when
    cart.addItem(item, false);

    // then
    assertThat(cart.getCartItemList().get(0).getItem()).isSameAs(item);
    assertThat(cart.getCartItemList().get(0).isInStock()).isFalse();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(1);
    assertThat(cart.getCartItemList().get(0).getTotal()).isEqualTo(new BigDecimal("2.05"));
  }

  @Test
  void removeItemByIdWhenItemNotFound() {
    // given
    Cart cart = new Cart();

    // when
    ItemSnapshot item = cart.removeItemById("I01");

    // then
    assertThat(item).isNull();
    assertThat(cart.containsItemId("I01")).isFalse();
    assertThat(cart.getNumberOfItems()).isEqualTo(0);
    assertThat(cart.getCartItems().hasNext()).isFalse();
    assertThat(cart.getAllCartItems().hasNext()).isFalse();
  }

  @Test
  void removeItemByIdWhenItemFound() {
    // given
    Cart cart = new Cart();
    ItemSnapshot item = item("I01", "2.05");
    cart.addItem(item, true);

    // when
    ItemSnapshot removedItem = cart.removeItemById("I01");

    // then
    assertThat(removedItem).isSameAs(item);
    assertThat(cart.getCartItemList()).isEmpty();
  }

  @Test
  void incrementQuantityByItemId() {
    // given
    Cart cart = new Cart();
    ItemSnapshot item = item("I01", "2.05");
    cart.addItem(item, true);

    // when
    cart.incrementQuantityByItemId("I01");
    cart.incrementQuantityByItemId("I01");

    // then
    assertThat(cart.getCartItemList().get(0).getItem()).isSameAs(item);
    assertThat(cart.getCartItemList().get(0).isInStock()).isTrue();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(3);
    assertThat(cart.getCartItemList().get(0).getTotal()).isEqualTo(new BigDecimal("6.15"));
  }

  @Test
  void setQuantityByItemId() {
    // given
    Cart cart = new Cart();
    ItemSnapshot item = item("I01", "2.05");
    cart.addItem(item, true);

    // when
    cart.setQuantityByItemId("I01", 10);

    // then
    assertThat(cart.getCartItemList().get(0).getItem()).isSameAs(item);
    assertThat(cart.getCartItemList().get(0).isInStock()).isTrue();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(10);
    assertThat(cart.getCartItemList().get(0).getTotal()).isEqualTo(new BigDecimal("20.50"));
  }

  @Test
  void getSubTotalWhenItemIsEmpty() {
    // given
    Cart cart = new Cart();

    // when
    BigDecimal subTotal = cart.getSubTotal();

    // then
    assertThat(subTotal).isEqualTo(BigDecimal.ZERO);

  }

  @Test
  void getSubTotalWhenItemIsExist() {
    // given
    Cart cart = new Cart();
    {
      cart.addItem(item("I01", "2.05"), true);
      cart.setQuantityByItemId("I01", 5);
    }
    {
      cart.addItem(item("I02", "3.06"), true);
      cart.setQuantityByItemId("I02", 6);
    }

    // when
    BigDecimal subTotal = cart.getSubTotal();

    // then
    assertThat(subTotal).isEqualTo(new BigDecimal("28.61"));
  }

  private static ItemSnapshot item(String itemId, String price) {
    return new ItemSnapshot(itemId, null, null, new BigDecimal(price), null, null, null, null, null, null);
  }

}

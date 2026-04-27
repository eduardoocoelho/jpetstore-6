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
package org.mybatis.jpetstore.order.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mybatis.jpetstore.account.api.CustomerProfile;
import org.mybatis.jpetstore.cart.api.CartLineSnapshot;
import org.mybatis.jpetstore.cart.api.CartSnapshot;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.catalog.api.ProductSummary;
import org.mybatis.jpetstore.domain.Order;

class OrderFactoryTest {

  private final OrderFactory orderFactory = new OrderFactory();

  @Test
  void shouldCreateOrderFromCustomerProfileAndCartSnapshot() {
    // given
    CustomerProfile customer = new CustomerProfile("mybatis", "mybatis@example.com", "My", "Batis", "Address 1",
        "Address 2", "City", "ST", "99001", "JPN", "09012345678", "FISH", "english");
    ProductSummary product = new ProductSummary("P01", "FISH", "Angelfish", "Fresh Water fish from China");
    ItemSnapshot item = new ItemSnapshot("I01", "P01", product, new BigDecimal("2.05"), "P", "Large", null, null, null,
        null);
    CartLineSnapshot cartLine = new CartLineSnapshot(item, 2, true, new BigDecimal("4.10"));
    CartSnapshot cart = new CartSnapshot(List.of(cartLine), new BigDecimal("4.10"));

    // when
    Order order = orderFactory.createOrder(customer, cart);

    // then
    assertThat(order.getUsername()).isEqualTo(customer.username());
    assertThat(order.getOrderDate()).isBeforeOrEqualsTo(new Date());
    assertThat(order.getShipAddress1()).isEqualTo(customer.address1());
    assertThat(order.getShipAddress2()).isEqualTo(customer.address2());
    assertThat(order.getShipCity()).isEqualTo(customer.city());
    assertThat(order.getShipState()).isEqualTo(customer.state());
    assertThat(order.getShipCountry()).isEqualTo(customer.country());
    assertThat(order.getShipZip()).isEqualTo(customer.zip());
    assertThat(order.getBillAddress1()).isEqualTo(customer.address1());
    assertThat(order.getBillAddress2()).isEqualTo(customer.address2());
    assertThat(order.getBillCity()).isEqualTo(customer.city());
    assertThat(order.getBillState()).isEqualTo(customer.state());
    assertThat(order.getBillCountry()).isEqualTo(customer.country());
    assertThat(order.getBillZip()).isEqualTo(customer.zip());
    assertThat(order.getTotalPrice()).isEqualTo(new BigDecimal("4.10"));
    assertThat(order.getCreditCard()).isEqualTo("999 9999 9999 9999");
    assertThat(order.getCardType()).isEqualTo("Visa");
    assertThat(order.getExpiryDate()).isEqualTo("12/03");
    assertThat(order.getCourier()).isEqualTo("UPS");
    assertThat(order.getLocale()).isEqualTo("CA");
    assertThat(order.getStatus()).isEqualTo("P");
    assertThat(order.getLineItems()).hasSize(1);
    assertThat(order.getLineItems().get(0).getLineNumber()).isEqualTo(1);
    assertThat(order.getLineItems().get(0).getItemId()).isEqualTo("I01");
    assertThat(order.getLineItems().get(0).getUnitPrice()).isEqualTo(new BigDecimal("2.05"));
    assertThat(order.getLineItems().get(0).getQuantity()).isEqualTo(2);
    assertThat(order.getLineItems().get(0).getTotal()).isEqualTo(new BigDecimal("4.10"));
    assertThat(order.getLineItems().get(0).getItem().getProduct().getProductId()).isEqualTo("P01");
  }

}

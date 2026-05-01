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

import java.util.Date;

import org.mybatis.jpetstore.account.api.CustomerProfile;
import org.mybatis.jpetstore.cart.api.CartLineSnapshot;
import org.mybatis.jpetstore.cart.api.CartSnapshot;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.catalog.api.ProductSummary;
import org.mybatis.jpetstore.catalog.domain.Item;
import org.mybatis.jpetstore.catalog.domain.Product;
import org.mybatis.jpetstore.domain.LineItem;
import org.mybatis.jpetstore.domain.Order;
import org.springframework.stereotype.Service;

@Service
public class OrderFactory {

  public Order createOrder(CustomerProfile customer, CartSnapshot cart) {
    Order order = new Order();

    order.setUsername(customer.username());
    order.setOrderDate(new Date());

    order.setShipToFirstName(customer.firstName());
    order.setShipToLastName(customer.lastName());
    order.setShipAddress1(customer.address1());
    order.setShipAddress2(customer.address2());
    order.setShipCity(customer.city());
    order.setShipState(customer.state());
    order.setShipZip(customer.zip());
    order.setShipCountry(customer.country());

    order.setBillToFirstName(customer.firstName());
    order.setBillToLastName(customer.lastName());
    order.setBillAddress1(customer.address1());
    order.setBillAddress2(customer.address2());
    order.setBillCity(customer.city());
    order.setBillState(customer.state());
    order.setBillZip(customer.zip());
    order.setBillCountry(customer.country());

    order.setTotalPrice(cart.subTotal());

    order.setCreditCard("999 9999 9999 9999");
    order.setExpiryDate("12/03");
    order.setCardType("Visa");
    order.setCourier("UPS");
    order.setLocale("CA");
    order.setStatus("P");

    cart.lines().forEach(cartLine -> order.addLineItem(toLineItem(order.getLineItems().size() + 1, cartLine)));
    return order;
  }

  private static LineItem toLineItem(int lineNumber, CartLineSnapshot cartLine) {
    LineItem lineItem = new LineItem();
    Item item = toItem(cartLine.item());
    lineItem.setLineNumber(lineNumber);
    lineItem.setItemId(item.getItemId());
    lineItem.setUnitPrice(item.getListPrice());
    lineItem.setQuantity(cartLine.quantity());
    lineItem.setItem(item);
    return lineItem;
  }

  private static Item toItem(ItemSnapshot itemSnapshot) {
    Item item = new Item();
    item.setItemId(itemSnapshot.itemId());
    item.setProduct(toProduct(itemSnapshot.product()));
    item.setListPrice(itemSnapshot.listPrice());
    item.setStatus(itemSnapshot.status());
    item.setAttribute1(itemSnapshot.attribute1());
    item.setAttribute2(itemSnapshot.attribute2());
    item.setAttribute3(itemSnapshot.attribute3());
    item.setAttribute4(itemSnapshot.attribute4());
    item.setAttribute5(itemSnapshot.attribute5());
    return item;
  }

  private static Product toProduct(ProductSummary productSummary) {
    if (productSummary == null) {
      return null;
    }
    Product product = new Product();
    product.setProductId(productSummary.productId());
    product.setCategoryId(productSummary.categoryId());
    product.setName(productSummary.name());
    product.setDescription(productSummary.description());
    return product;
  }

}

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
package org.mybatis.jpetstore.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;

import org.mybatis.jpetstore.cart.domain.CartItem;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.catalog.api.ProductSummary;
import org.mybatis.jpetstore.catalog.domain.Item;
import org.mybatis.jpetstore.catalog.domain.Product;

/**
 * The Class LineItem.
 *
 * @author Eduardo Macarron
 */
public class LineItem implements Serializable {

  private static final long serialVersionUID = 6804536240033522156L;

  private int orderId;
  private int lineNumber;
  private int quantity;
  private String itemId;
  private BigDecimal unitPrice;
  private Item item;
  private BigDecimal total;

  public LineItem() {
  }

  /**
   * Instantiates a new line item.
   *
   * @param lineNumber
   *          the line number
   * @param cartItem
   *          the cart item
   */
  public LineItem(int lineNumber, CartItem cartItem) {
    ItemSnapshot cartItemSnapshot = cartItem.getItem();
    this.lineNumber = lineNumber;
    this.quantity = cartItem.getQuantity();
    this.itemId = cartItemSnapshot.itemId();
    this.unitPrice = cartItemSnapshot.listPrice();
    this.item = toItem(cartItemSnapshot);
    calculateTotal();
  }

  public int getOrderId() {
    return orderId;
  }

  public void setOrderId(int orderId) {
    this.orderId = orderId;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitprice) {
    this.unitPrice = unitprice;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
    calculateTotal();
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
    calculateTotal();
  }

  private void calculateTotal() {
    total = Optional.ofNullable(item).map(Item::getListPrice).map(v -> v.multiply(new BigDecimal(quantity)))
        .orElse(null);
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

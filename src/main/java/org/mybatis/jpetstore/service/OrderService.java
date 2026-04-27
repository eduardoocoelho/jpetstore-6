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
package org.mybatis.jpetstore.service;

import java.util.List;

import org.mybatis.jpetstore.catalog.api.CatalogQueryService;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.catalog.api.ProductSummary;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.domain.Order;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.domain.Sequence;
import org.mybatis.jpetstore.inventory.api.InventoryQueryService;
import org.mybatis.jpetstore.inventory.api.InventoryReservationService;
import org.mybatis.jpetstore.mapper.LineItemMapper;
import org.mybatis.jpetstore.mapper.OrderMapper;
import org.mybatis.jpetstore.mapper.SequenceMapper;
import org.mybatis.jpetstore.order.api.OrderQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class OrderService.
 *
 * @author Eduardo Macarron
 */
@Service
public class OrderService implements OrderQueryService {

  private final CatalogQueryService catalogQueryService;
  private final InventoryQueryService inventoryQueryService;
  private final InventoryReservationService inventoryReservationService;
  private final OrderMapper orderMapper;
  private final SequenceMapper sequenceMapper;
  private final LineItemMapper lineItemMapper;

  public OrderService(CatalogQueryService catalogQueryService, InventoryQueryService inventoryQueryService,
      InventoryReservationService inventoryReservationService, OrderMapper orderMapper, SequenceMapper sequenceMapper,
      LineItemMapper lineItemMapper) {
    this.catalogQueryService = catalogQueryService;
    this.inventoryQueryService = inventoryQueryService;
    this.inventoryReservationService = inventoryReservationService;
    this.orderMapper = orderMapper;
    this.sequenceMapper = sequenceMapper;
    this.lineItemMapper = lineItemMapper;
  }

  /**
   * Insert order.
   *
   * @param order
   *          the order
   */
  @Transactional
  public void insertOrder(Order order) {
    order.setOrderId(getNextId("ordernum"));
    order.getLineItems().forEach(lineItem -> {
      inventoryReservationService.decrement(lineItem.getItemId(), lineItem.getQuantity());
    });

    orderMapper.insertOrder(order);
    orderMapper.insertOrderStatus(order);
    order.getLineItems().forEach(lineItem -> {
      lineItem.setOrderId(order.getOrderId());
      lineItemMapper.insertLineItem(lineItem);
    });
  }

  /**
   * Gets the order.
   *
   * @param orderId
   *          the order id
   *
   * @return the order
   */
  @Override
  @Transactional
  public Order getOrder(int orderId) {
    Order order = orderMapper.getOrder(orderId);
    order.setLineItems(lineItemMapper.getLineItemsByOrderId(orderId));

    order.getLineItems().forEach(lineItem -> {
      Item item = toItem(catalogQueryService.getItemSnapshot(lineItem.getItemId()));
      item.setQuantity(inventoryQueryService.getQuantity(lineItem.getItemId()));
      lineItem.setItem(item);
    });

    return order;
  }

  /**
   * Gets the orders by username.
   *
   * @param username
   *          the username
   *
   * @return the orders by username
   */
  @Override
  public List<Order> getOrdersByUsername(String username) {
    return orderMapper.getOrdersByUsername(username);
  }

  /**
   * Gets the next id.
   *
   * @param name
   *          the name
   *
   * @return the next id
   */
  public int getNextId(String name) {
    Sequence sequence = sequenceMapper.getSequence(new Sequence(name, -1));
    if (sequence == null) {
      throw new RuntimeException(
          "Error: A null sequence was returned from the database (could not get next " + name + " sequence).");
    }
    Sequence parameterObject = new Sequence(name, sequence.getNextId() + 1);
    sequenceMapper.updateSequence(parameterObject);
    return sequence.getNextId();
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

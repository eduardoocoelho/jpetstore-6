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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.jpetstore.catalog.api.CatalogQueryService;
import org.mybatis.jpetstore.inventory.application.InventoryService;
import org.mybatis.jpetstore.inventory.persistence.InventoryMapper;
import org.mybatis.jpetstore.order.domain.LineItem;
import org.mybatis.jpetstore.order.domain.Order;
import org.mybatis.jpetstore.order.persistence.LineItemMapper;
import org.mybatis.jpetstore.order.persistence.OrderMapper;
import org.mybatis.jpetstore.order.persistence.SequenceMapper;
import org.mybatis.jpetstore.shared.persistence.MapperTestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OrderServiceTransactionTest.TestConfig.class)
class OrderServiceTransactionTest {

  @Autowired
  private OrderService orderService;

  @Autowired
  private DataSource dataSource;

  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Test
  void insertOrderRollsBackInventoryDecrementWhenOrderPersistenceFails() {
    String itemId = "EST-1";
    int quantityBefore = getInventoryQuantity(itemId);
    Order order = new Order();
    LineItem lineItem = new LineItem();
    lineItem.setItemId(itemId);
    lineItem.setQuantity(1);
    order.addLineItem(lineItem);

    assertThatThrownBy(() -> orderService.insertOrder(order)).isInstanceOf(RuntimeException.class);

    assertThat(getInventoryQuantity(itemId)).isEqualTo(quantityBefore);
  }

  private int getInventoryQuantity(String itemId) {
    return jdbcTemplate.queryForObject("SELECT QTY FROM inventory WHERE itemid = ?", Integer.class, itemId);
  }

  @Configuration
  @EnableTransactionManagement(proxyTargetClass = true)
  @Import(MapperTestContext.class)
  static class TestConfig {

    @Bean
    CatalogQueryService catalogQueryService() {
      return org.mockito.Mockito.mock(CatalogQueryService.class);
    }

    @Bean
    InventoryService inventoryService(InventoryMapper inventoryMapper) {
      return new InventoryService(inventoryMapper);
    }

    @Bean
    OrderService orderService(CatalogQueryService catalogQueryService, InventoryService inventoryService,
        OrderMapper orderMapper, SequenceMapper sequenceMapper, LineItemMapper lineItemMapper) {
      return new OrderService(catalogQueryService, inventoryService, orderMapper, sequenceMapper, lineItemMapper);
    }
  }
}

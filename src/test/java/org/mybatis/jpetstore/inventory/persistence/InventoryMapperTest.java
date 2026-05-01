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
package org.mybatis.jpetstore.inventory.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.jpetstore.shared.persistence.MapperTestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapperTestContext.class)
@Transactional
class InventoryMapperTest {

  @Autowired
  private InventoryMapper mapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void getInventoryQuantity() {
    // given
    String itemId = "EST-1";

    // when
    int quantity = mapper.getInventoryQuantity(itemId);

    // then
    assertThat(quantity).isEqualTo(10000);
  }

  @Test
  void updateInventoryQuantity() {
    // given
    String itemId = "EST-1";
    Map<String, Object> params = new HashMap<>();
    params.put("itemId", itemId);
    params.put("increment", 10);

    // when
    mapper.updateInventoryQuantity(params);

    // then
    Integer quantity = jdbcTemplate.queryForObject("SELECT QTY FROM inventory WHERE itemid = ?", Integer.class, itemId);
    assertThat(quantity).isEqualTo(9990);
  }

}

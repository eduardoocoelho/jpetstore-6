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
package org.mybatis.jpetstore.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrderTest {

  @Test
  void shouldAddLineItem() {
    Order order = new Order();
    LineItem lineItem = new LineItem();
    lineItem.setLineNumber(1);

    order.addLineItem(lineItem);

    assertThat(order.getLineItems()).containsExactly(lineItem);
  }

  @Test
  void shouldSetOrderDate() {
    Order order = new Order();
    Date orderDate = new Date();

    order.setOrderDate(orderDate);

    assertThat(order.getOrderDate()).isSameAs(orderDate);
  }

  @Test
  void shouldNotExposeAccountOrCartDomainTypes() {
    List<String> references = new ArrayList<>();

    collectForbiddenReferences(references, Order.class);
    collectForbiddenReferences(references, LineItem.class);

    assertThat(references).isEmpty();
  }

  private static void collectForbiddenReferences(List<String> references, Class<?> type) {
    for (Field field : type.getDeclaredFields()) {
      addIfForbidden(references, type.getSimpleName() + "." + field.getName(), field.getType());
    }
    for (Method method : type.getDeclaredMethods()) {
      addIfForbidden(references, type.getSimpleName() + "." + method.getName() + " return", method.getReturnType());
      collectExecutableReferences(references, type.getSimpleName() + "." + method.getName(), method);
    }
    for (Executable constructor : type.getDeclaredConstructors()) {
      collectExecutableReferences(references, type.getSimpleName() + " constructor", constructor);
    }
  }

  private static void collectExecutableReferences(List<String> references, String memberName, Executable executable) {
    for (Class<?> parameterType : executable.getParameterTypes()) {
      addIfForbidden(references, memberName + " parameter", parameterType);
    }
  }

  private static void addIfForbidden(List<String> references, String memberName, Class<?> type) {
    if (isForbiddenDomainType(type)) {
      references.add(memberName + " references " + type.getName());
    }
  }

  private static boolean isForbiddenDomainType(Class<?> type) {
    String name = type.getName();
    return name.startsWith("org.mybatis.jpetstore.account.domain.")
        || name.startsWith("org.mybatis.jpetstore.cart.domain.");
  }

}

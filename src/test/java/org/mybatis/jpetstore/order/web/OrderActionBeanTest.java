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
package org.mybatis.jpetstore.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.controller.ActionResolver;
import net.sourceforge.stripes.controller.StripesFilter;

import org.junit.jupiter.api.Test;
import org.mybatis.jpetstore.account.domain.Account;
import org.mybatis.jpetstore.account.web.AccountActionBean;
import org.mybatis.jpetstore.cart.domain.Cart;
import org.mybatis.jpetstore.cart.web.CartActionBean;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.order.application.OrderFactory;
import org.mybatis.jpetstore.order.application.OrderService;
import org.mybatis.jpetstore.order.domain.Order;
import org.springframework.test.util.ReflectionTestUtils;

class OrderActionBeanTest {

  // Test written by Diffblue Cover.
  @Test
  void getOrderListOutputNull() {

    // Arrange
    final OrderActionBean orderActionBean = new OrderActionBean();

    // Act and Assert result
    assertThat(orderActionBean.getOrderList()).isNull();

  }

  // Test written by Diffblue Cover.
  @Test
  void isShippingAddressRequiredOutputFalse() {

    // Arrange
    final OrderActionBean orderActionBean = new OrderActionBean();

    // Act and Assert result
    assertThat(orderActionBean.isShippingAddressRequired()).isFalse();

  }

  // Test written by Diffblue Cover.
  @Test
  void constructorOutputNotNull() {

    // Act, creating object to test constructor
    final OrderActionBean actual = new OrderActionBean();

    // Assert result
    assertThat(actual).isNotNull().isNotNull();
    assertThat(actual.getContext()).isNull();

  }

  // Test written by Diffblue Cover.
  @Test
  void isConfirmedOutputFalse() {

    // Arrange
    final OrderActionBean orderActionBean = new OrderActionBean();

    // Act and Assert result
    assertThat(orderActionBean.isConfirmed()).isFalse();

  }

  @Test
  void listOrdersUsesAuthenticatedAccountUsernameFromSession() {
    OrderActionBean orderActionBean = new OrderActionBean();
    OrderService orderService = mock(OrderService.class);
    HttpSession session = sessionFor(orderActionBean);
    AccountActionBean accountBean = authenticatedAccountBean("j2ee");
    List<Order> expectedOrders = List.of(new Order());

    ReflectionTestUtils.setField(orderActionBean, "orderService", orderService);
    when(session.getAttribute("/actions/Account.action")).thenReturn(accountBean);
    when(orderService.getOrdersByUsername("j2ee")).thenReturn(expectedOrders);

    Resolution resolution = orderActionBean.listOrders();

    assertThat(resolution.toString()).contains("ListOrders.jsp");
    assertThat(orderActionBean.getOrderList()).isSameAs(expectedOrders);
  }

  @Test
  void newOrderFormCreatesOrderFromAuthenticatedAccountAndSessionCart() {
    OrderActionBean orderActionBean = new OrderActionBean();
    HttpSession session = sessionFor(orderActionBean);
    AccountActionBean accountBean = authenticatedAccountBean("j2ee");
    CartActionBean cartBean = cartBeanWithItem("EST-1", "16.50", 2);

    ReflectionTestUtils.setField(orderActionBean, "orderFactory", new OrderFactory());
    when(session.getAttribute("/actions/Account.action")).thenReturn(accountBean);
    when(session.getAttribute("/actions/Cart.action")).thenReturn(cartBean);

    Resolution resolution = orderActionBean.newOrderForm();

    assertThat(resolution.toString()).contains("NewOrderForm.jsp");
    assertThat(orderActionBean.getOrder().getUsername()).isEqualTo("j2ee");
    assertThat(orderActionBean.getOrder().getBillToFirstName()).isEqualTo("Jane");
    assertThat(orderActionBean.getOrder().getShipToLastName()).isEqualTo("Doe");
    assertThat(orderActionBean.getOrder().getTotalPrice()).isEqualTo(new BigDecimal("33.00"));
    assertThat(orderActionBean.getOrder().getLineItems()).hasSize(1);
    assertThat(orderActionBean.getOrder().getLineItems().get(0).getItemId()).isEqualTo("EST-1");
    assertThat(orderActionBean.getOrder().getLineItems().get(0).getQuantity()).isEqualTo(2);
  }

  @Test
  void newOrderFormRejectsUnauthenticatedCheckout() {
    OrderActionBean orderActionBean = new OrderActionBean();
    ActionBeanContext context = contextWithRequestAndMessages();
    HttpSession session = context.getRequest().getSession();
    AccountActionBean accountBean = new AccountActionBean();

    orderActionBean.setContext(context);
    when(session.getAttribute("/actions/Account.action")).thenReturn(accountBean);
    configureStripesActionResolver();

    Resolution resolution = orderActionBean.newOrderForm();

    assertThat(resolution.toString()).contains("Account.action");
    assertThat(context.getMessages()).extracting(message -> message.getMessage(Locale.getDefault())).containsExactly(
        "You must sign on before attempting to check out.  Please sign on and try checking out again.");
  }

  @Test
  void confirmedNewOrderSubmitsOrderClearsCartAndShowsSubmittedMessage() {
    OrderActionBean orderActionBean = new OrderActionBean();
    OrderService orderService = mock(OrderService.class);
    ActionBeanContext context = contextWithRequestAndMessages();
    HttpSession session = context.getRequest().getSession();
    CartActionBean cartBean = cartBeanWithItem("EST-1", "16.50", 1);
    Order order = new Order();

    ReflectionTestUtils.setField(orderActionBean, "orderService", orderService);
    orderActionBean.setContext(context);
    orderActionBean.setOrder(order);
    orderActionBean.setConfirmed(true);
    when(session.getAttribute("/actions/Cart.action")).thenReturn(cartBean);

    Resolution resolution = orderActionBean.newOrder();

    assertThat(resolution.toString()).contains("ViewOrder.jsp");
    verify(orderService).insertOrder(order);
    assertThat(cartBean.getCart().getNumberOfItems()).isZero();
    assertThat(context.getMessages()).extracting(message -> message.getMessage(Locale.getDefault()))
        .containsExactly("Thank you, your order has been submitted.");
  }

  @Test
  void viewOrderRejectsOrderOwnedByAnotherUser() {
    OrderActionBean orderActionBean = new OrderActionBean();
    OrderService orderService = mock(OrderService.class);
    ActionBeanContext context = contextWithRequestAndMessages();
    HttpSession session = context.getRequest().getSession();
    Order persistedOrder = new Order();
    persistedOrder.setOrderId(1001);
    persistedOrder.setUsername("other-user");

    ReflectionTestUtils.setField(orderActionBean, "orderService", orderService);
    orderActionBean.setContext(context);
    orderActionBean.setOrderId(1001);
    when(session.getAttribute("accountBean")).thenReturn(authenticatedAccountBean("j2ee"));
    when(orderService.getOrder(1001)).thenReturn(persistedOrder);

    Resolution resolution = orderActionBean.viewOrder();

    assertThat(resolution.toString()).contains("Error.jsp");
    assertThat(orderActionBean.getOrder()).isNull();
    assertThat(context.getMessages()).extracting(message -> message.getMessage(Locale.getDefault()))
        .containsExactly("You may only view your own orders.");
  }

  @SuppressWarnings("unchecked")
  private static void configureStripesActionResolver() {
    Configuration configuration = mock(Configuration.class);
    ActionResolver actionResolver = mock(ActionResolver.class);
    ThreadLocal<Configuration> configurationStash = (ThreadLocal<Configuration>) ReflectionTestUtils
        .getField(StripesFilter.class, "configurationStash");
    when(configuration.getActionResolver()).thenReturn(actionResolver);
    when(actionResolver.getUrlBinding(AccountActionBean.class)).thenReturn("/actions/Account.action");
    configurationStash.set(configuration);
  }

  private static HttpSession sessionFor(OrderActionBean orderActionBean) {
    ActionBeanContext context = contextWithRequestAndMessages();
    orderActionBean.setContext(context);
    return context.getRequest().getSession();
  }

  private static ActionBeanContext contextWithRequestAndMessages() {
    ActionBeanContext context = mock(ActionBeanContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(context.getRequest()).thenReturn(request);
    when(context.getMessages()).thenReturn(new ArrayList<Message>());
    when(request.getSession()).thenReturn(session);
    return context;
  }

  private static AccountActionBean authenticatedAccountBean(String username) {
    Account account = new Account();
    account.setUsername(username);
    account.setFirstName("Jane");
    account.setLastName("Doe");
    account.setAddress1("1 Main Street");
    account.setAddress2("Apt 2");
    account.setCity("Denver");
    account.setState("CO");
    account.setZip("80202");
    account.setCountry("USA");

    AccountActionBean accountBean = new AccountActionBean();
    ReflectionTestUtils.setField(accountBean, "account", account);
    ReflectionTestUtils.setField(accountBean, "authenticated", true);
    return accountBean;
  }

  private static CartActionBean cartBeanWithItem(String itemId, String price, int quantity) {
    ItemSnapshot item = new ItemSnapshot(itemId, null, null, new BigDecimal(price), null, null, null, null, null, null);

    Cart cart = new Cart();
    cart.addItem(item, true);
    cart.setQuantityByItemId(itemId, quantity);

    CartActionBean cartBean = new CartActionBean();
    cartBean.setCart(cart);
    return cartBean;
  }
}

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
package org.mybatis.jpetstore.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.mybatis.jpetstore.account.api.CustomerProfile;
import org.mybatis.jpetstore.cart.api.CartSnapshot;
import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.web.actions.AccountActionBean;
import org.mybatis.jpetstore.web.actions.CartActionBean;
import org.springframework.test.util.ReflectionTestUtils;

class SessionStateTest {

  @Test
  void shouldReportAnonymousSessionWhenNoAccountBeanExists() {
    HttpSession session = mock(HttpSession.class);
    SessionState sessionState = new SessionState(session);

    assertThat(sessionState.isAuthenticated()).isFalse();
    assertThat(sessionState.getCurrentUsername()).isNull();
    assertThat(sessionState.getCurrentCustomerProfile()).isNull();
  }

  @Test
  void shouldReadAuthenticatedAccountFromPrimarySessionKey() {
    HttpSession session = mock(HttpSession.class);
    AccountActionBean accountBean = authenticatedAccountBean();
    when(session.getAttribute(SessionState.ACCOUNT_ACTION_SESSION_KEY)).thenReturn(accountBean);
    SessionState sessionState = new SessionState(session);

    CustomerProfile customerProfile = sessionState.getCurrentCustomerProfile();

    assertThat(sessionState.isAuthenticated()).isTrue();
    assertThat(sessionState.getCurrentUsername()).isEqualTo("j2ee");
    assertThat(customerProfile.username()).isEqualTo("j2ee");
    assertThat(customerProfile.email()).isEqualTo("j2ee@example.com");
    assertThat(customerProfile.firstName()).isEqualTo("Jane");
    assertThat(customerProfile.lastName()).isEqualTo("Doe");
    assertThat(customerProfile.address1()).isEqualTo("1 Main Street");
    assertThat(customerProfile.address2()).isEqualTo("Apt 2");
    assertThat(customerProfile.city()).isEqualTo("Denver");
    assertThat(customerProfile.state()).isEqualTo("CO");
    assertThat(customerProfile.zip()).isEqualTo("80202");
    assertThat(customerProfile.country()).isEqualTo("USA");
    assertThat(customerProfile.phone()).isEqualTo("555-0100");
    assertThat(customerProfile.favouriteCategoryId()).isEqualTo("DOGS");
    assertThat(customerProfile.languagePreference()).isEqualTo("english");
  }

  @Test
  void shouldFallBackToAccountAliasSessionKey() {
    HttpSession session = mock(HttpSession.class);
    AccountActionBean accountBean = authenticatedAccountBean();
    when(session.getAttribute(SessionState.ACCOUNT_ACTION_SESSION_KEY)).thenReturn(null);
    when(session.getAttribute(SessionState.ACCOUNT_ALIAS_SESSION_KEY)).thenReturn(accountBean);
    SessionState sessionState = new SessionState(session);

    assertThat(sessionState.isAuthenticated()).isTrue();
    assertThat(sessionState.getCurrentUsername()).isEqualTo("j2ee");
  }

  @Test
  void shouldReadCartSnapshotFromCartActionSessionKey() {
    HttpSession session = mock(HttpSession.class);
    CartActionBean cartBean = cartActionBeanWithItem();
    when(session.getAttribute(SessionState.CART_ACTION_SESSION_KEY)).thenReturn(cartBean);
    SessionState sessionState = new SessionState(session);

    CartSnapshot cartSnapshot = sessionState.getCurrentCartSnapshot();

    assertThat(cartSnapshot.numberOfItems()).isEqualTo(1);
    assertThat(cartSnapshot.subTotal()).isEqualTo(new BigDecimal("33.00"));
    assertThat(cartSnapshot.lines()).hasSize(1);
    assertThat(cartSnapshot.lines().get(0).item().itemId()).isEqualTo("EST-1");
    assertThat(cartSnapshot.lines().get(0).item().product().productId()).isEqualTo("FI-SW-01");
    assertThat(cartSnapshot.lines().get(0).quantity()).isEqualTo(2);
    assertThat(cartSnapshot.lines().get(0).inStock()).isTrue();
  }

  @Test
  void shouldReturnNullCartSnapshotWhenCartBeanIsMissing() {
    HttpSession session = mock(HttpSession.class);
    SessionState sessionState = new SessionState(session);

    assertThat(sessionState.getCurrentCartSnapshot()).isNull();
  }

  @Test
  void shouldClearCartBeanWhenPresent() {
    HttpSession session = mock(HttpSession.class);
    CartActionBean cartBean = cartActionBeanWithItem();
    when(session.getAttribute(SessionState.CART_ACTION_SESSION_KEY)).thenReturn(cartBean);
    SessionState sessionState = new SessionState(session);

    sessionState.clearCart();

    assertThat(cartBean.getCart().getNumberOfItems()).isZero();
  }

  private static AccountActionBean authenticatedAccountBean() {
    Account account = new Account();
    account.setUsername("j2ee");
    account.setEmail("j2ee@example.com");
    account.setFirstName("Jane");
    account.setLastName("Doe");
    account.setAddress1("1 Main Street");
    account.setAddress2("Apt 2");
    account.setCity("Denver");
    account.setState("CO");
    account.setZip("80202");
    account.setCountry("USA");
    account.setPhone("555-0100");
    account.setFavouriteCategoryId("DOGS");
    account.setLanguagePreference("english");

    AccountActionBean accountBean = new AccountActionBean();
    ReflectionTestUtils.setField(accountBean, "account", account);
    ReflectionTestUtils.setField(accountBean, "authenticated", true);
    return accountBean;
  }

  private static CartActionBean cartActionBeanWithItem() {
    Product product = new Product();
    product.setProductId("FI-SW-01");
    product.setCategoryId("FISH");
    product.setName("Angelfish");
    product.setDescription("Fresh Water fish from China");

    Item item = new Item();
    item.setItemId("EST-1");
    item.setProduct(product);
    item.setListPrice(new BigDecimal("16.50"));

    Cart cart = new Cart();
    cart.addItem(item, true);
    cart.setQuantityByItemId("EST-1", 2);

    CartActionBean cartBean = new CartActionBean();
    cartBean.setCart(cart);
    return cartBean;
  }

}

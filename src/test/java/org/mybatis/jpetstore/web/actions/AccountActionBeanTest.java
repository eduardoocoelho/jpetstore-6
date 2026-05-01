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
package org.mybatis.jpetstore.web.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mybatis.jpetstore.catalog.application.CatalogService;
import org.mybatis.jpetstore.catalog.domain.Product;
import org.mybatis.jpetstore.catalog.web.CatalogActionBean;
import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.service.AccountService;
import org.springframework.test.util.ReflectionTestUtils;

class AccountActionBeanTest {

  // Test written by Diffblue Cover.
  @Test
  void getMyListOutputNull() {

    // Arrange
    final AccountActionBean accountActionBean = new AccountActionBean();

    // Act and Assert result
    assertThat(accountActionBean.getMyList()).isNull();

  }

  // Test written by Diffblue Cover.
  @Test
  void constructorOutputNotNull() {

    // Act, creating object to test constructor
    final AccountActionBean actual = new AccountActionBean();

    // Assert result
    assertThat(actual).isNotNull();
    assertThat(actual.getContext()).isNull();

  }

  // Test written by Diffblue Cover.
  @Test
  void getPasswordOutputNull() {

    // Arrange
    final AccountActionBean accountActionBean = new AccountActionBean();

    // Act and Assert result
    assertThat(accountActionBean.getPassword()).isNull();

  }

  // Test written by Diffblue Cover.
  @Test
  void isAuthenticatedOutputFalse() {

    // Arrange
    final AccountActionBean accountActionBean = new AccountActionBean();

    // Act and Assert result
    assertThat(accountActionBean.isAuthenticated()).isFalse();

  }

  // Test written by Diffblue Cover.
  @Test
  void getUsernameOutputNull() {

    // Arrange
    final AccountActionBean accountActionBean = new AccountActionBean();

    // Act and Assert result
    assertThat(accountActionBean.getUsername()).isNull();

  }

  // Test written by Diffblue Cover.
  @Test
  void getAccountOutputNotNull() {

    // Arrange
    final AccountActionBean accountActionBean = new AccountActionBean();

    // Act
    final Account actual = accountActionBean.getAccount();

    // Assert result
    assertThat(actual).isNotNull();
    assertThat(actual.getAddress2()).isNull();
    assertThat(actual.getState()).isNull();
    assertThat(actual.getFirstName()).isNull();
    assertThat(actual.getPassword()).isNull();
    assertThat(actual.getLanguagePreference()).isNull();
    assertThat(actual.getFavouriteCategoryId()).isNull();
    assertThat(actual.getCountry()).isNull();
    assertThat(actual.getPhone()).isNull();
    assertThat(actual.getUsername()).isNull();
    assertThat(actual.getLastName()).isNull();
    assertThat(actual.getAddress1()).isNull();
    assertThat(actual.getEmail()).isNull();
    assertThat(actual.getStatus()).isNull();
    assertThat(actual.getBannerName()).isNull();
    assertThat(actual.getZip()).isNull();
    assertThat(actual.getCity()).isNull();

  }

  @Test
  void signonLoadsAccountFavoriteCategoryProductListAndRegistersSessionAlias() {
    AccountActionBean accountActionBean = new AccountActionBean();
    AccountService accountService = mock(AccountService.class);
    CatalogService catalogService = mock(CatalogService.class);
    ActionBeanContext context = mock(ActionBeanContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    List<Product> favoriteProducts = List.of(new Product());

    Account account = new Account();
    account.setUsername("j2ee");
    account.setPassword("j2ee");
    account.setFirstName("ABC");
    account.setFavouriteCategoryId("DOGS");

    ReflectionTestUtils.setField(accountActionBean, "accountService", accountService);
    ReflectionTestUtils.setField(accountActionBean, "catalogService", catalogService);
    when(context.getRequest()).thenReturn(request);
    when(request.getSession()).thenReturn(session);
    when(accountService.getAccount("j2ee", "j2ee")).thenReturn(account);
    when(catalogService.getProductListByCategory("DOGS")).thenReturn(favoriteProducts);
    configureStripesActionResolver();
    accountActionBean.setContext(context);
    accountActionBean.setUsername("j2ee");
    accountActionBean.setPassword("j2ee");

    Resolution resolution = accountActionBean.signon();

    assertThat(resolution.toString()).contains("Catalog.action");
    assertThat(accountActionBean.isAuthenticated()).isTrue();
    assertThat(accountActionBean.getAccount()).isSameAs(account);
    assertThat(accountActionBean.getPassword()).isNull();
    assertThat(accountActionBean.getMyList()).isSameAs(favoriteProducts);
    verify(session).setAttribute("accountBean", accountActionBean);
  }

  @Test
  void signonFailureClearsAccountAndKeepsFavoriteListEmpty() {
    AccountActionBean accountActionBean = new AccountActionBean();
    AccountService accountService = mock(AccountService.class);
    ActionBeanContext context = mock(ActionBeanContext.class);

    ReflectionTestUtils.setField(accountActionBean, "accountService", accountService);
    when(context.getMessages()).thenReturn(new ArrayList<Message>());
    when(accountService.getAccount("bad-user", "bad-password")).thenReturn(null);
    accountActionBean.setContext(context);
    accountActionBean.setUsername("bad-user");
    accountActionBean.setPassword("bad-password");

    Resolution resolution = accountActionBean.signon();

    assertThat(resolution.toString()).contains("SignonForm.jsp");
    assertThat(accountActionBean.isAuthenticated()).isFalse();
    assertThat(accountActionBean.getUsername()).isNull();
    assertThat(accountActionBean.getMyList()).isNull();
    assertThat(context.getMessages()).extracting(message -> message.getMessage(Locale.getDefault()))
        .containsExactly("Invalid username or password.  Signon failed.");
  }

  @SuppressWarnings("unchecked")
  private static void configureStripesActionResolver() {
    Configuration configuration = mock(Configuration.class);
    ActionResolver actionResolver = mock(ActionResolver.class);
    ThreadLocal<Configuration> configurationStash = (ThreadLocal<Configuration>) ReflectionTestUtils
        .getField(StripesFilter.class, "configurationStash");
    when(configuration.getActionResolver()).thenReturn(actionResolver);
    when(actionResolver.getUrlBinding(CatalogActionBean.class)).thenReturn("/actions/Catalog.action");
    configurationStash.set(configuration);
  }
}

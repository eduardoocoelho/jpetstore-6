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

import javax.servlet.http.HttpSession;

public class SessionState {

  public static final String ACCOUNT_ACTION_SESSION_KEY = "/actions/Account.action";
  public static final String CART_ACTION_SESSION_KEY = "/actions/Cart.action";
  public static final String ACCOUNT_ALIAS_SESSION_KEY = "accountBean";

  private final HttpSession session;

  public SessionState(HttpSession session) {
    this.session = session;
  }

  public boolean isAuthenticated() {
    SessionAccount account = getAccount();
    return account != null && account.isAuthenticated();
  }

  public String getCurrentUsername() {
    SessionAccount account = getAccount();
    return account == null ? null : account.getCurrentUsername();
  }

  public <T> T getCurrentCustomerProfile(Class<T> type) {
    SessionAccount account = getAccount();
    if (account == null) {
      return null;
    }
    return type.cast(account.getCurrentCustomerProfile());
  }

  public <T> T getCurrentCartSnapshot(Class<T> type) {
    SessionCart cart = getCart();
    if (cart == null) {
      return null;
    }
    return type.cast(cart.getCurrentCartSnapshot());
  }

  public void clearCart() {
    SessionCart cart = getCart();
    if (cart != null) {
      cart.clearCart();
    }
  }

  private SessionAccount getAccount() {
    Object account = session.getAttribute(ACCOUNT_ACTION_SESSION_KEY);
    if (account == null) {
      account = session.getAttribute(ACCOUNT_ALIAS_SESSION_KEY);
    }
    if (account == null) {
      return null;
    }
    return (SessionAccount) account;
  }

  private SessionCart getCart() {
    Object cart = session.getAttribute(CART_ACTION_SESSION_KEY);
    if (cart == null) {
      return null;
    }
    return (SessionCart) cart;
  }

}

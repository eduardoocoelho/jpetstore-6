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

import java.math.BigDecimal;
import java.util.Iterator;

import javax.servlet.http.HttpSession;

import org.mybatis.jpetstore.account.api.CustomerProfile;
import org.mybatis.jpetstore.cart.api.CartQueryService;
import org.mybatis.jpetstore.cart.api.CartSnapshot;
import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.CartItem;
import org.mybatis.jpetstore.web.actions.AccountActionBean;
import org.mybatis.jpetstore.web.actions.CartActionBean;

public class SessionState {

  public static final String ACCOUNT_ACTION_SESSION_KEY = "/actions/Account.action";
  public static final String CART_ACTION_SESSION_KEY = "/actions/Cart.action";
  public static final String ACCOUNT_ALIAS_SESSION_KEY = "accountBean";

  private static final CartQueryService CART_QUERY_SERVICE = new SessionCartQueryService();

  private final HttpSession session;

  public SessionState(HttpSession session) {
    this.session = session;
  }

  public boolean isAuthenticated() {
    AccountActionBean accountBean = getAccountActionBean();
    return accountBean != null && accountBean.isAuthenticated();
  }

  public String getCurrentUsername() {
    AccountActionBean accountBean = getAccountActionBean();
    if (accountBean == null || accountBean.getAccount() == null) {
      return null;
    }
    return accountBean.getAccount().getUsername();
  }

  public CustomerProfile getCurrentCustomerProfile() {
    AccountActionBean accountBean = getAccountActionBean();
    if (accountBean == null) {
      return null;
    }
    return toCustomerProfile(accountBean.getAccount());
  }

  public CartSnapshot getCurrentCartSnapshot() {
    CartActionBean cartBean = getCartActionBean();
    if (cartBean == null || cartBean.getCart() == null) {
      return null;
    }
    return CART_QUERY_SERVICE.getCartSnapshot(cartBean.getCart());
  }

  public void clearCart() {
    CartActionBean cartBean = getCartActionBean();
    if (cartBean != null) {
      cartBean.clear();
    }
  }

  private AccountActionBean getAccountActionBean() {
    Object accountBean = session.getAttribute(ACCOUNT_ACTION_SESSION_KEY);
    if (accountBean == null) {
      accountBean = session.getAttribute(ACCOUNT_ALIAS_SESSION_KEY);
    }
    return (AccountActionBean) accountBean;
  }

  private CartActionBean getCartActionBean() {
    return (CartActionBean) session.getAttribute(CART_ACTION_SESSION_KEY);
  }

  private static CustomerProfile toCustomerProfile(Account account) {
    if (account == null) {
      return null;
    }
    return new CustomerProfile(account.getUsername(), account.getEmail(), account.getFirstName(), account.getLastName(),
        account.getAddress1(), account.getAddress2(), account.getCity(), account.getState(), account.getZip(),
        account.getCountry(), account.getPhone(), account.getFavouriteCategoryId(), account.getLanguagePreference());
  }

  private static class SessionCartQueryService implements CartQueryService {

    @Override
    public Iterator<CartItem> getAllCartItems(Cart cart) {
      return cart.getAllCartItems();
    }

    @Override
    public int getNumberOfItems(Cart cart) {
      return cart.getNumberOfItems();
    }

    @Override
    public BigDecimal getSubTotal(Cart cart) {
      return cart.getSubTotal();
    }
  }

}

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

public class SharedPageView {

  private final AccountSessionView account;
  private final HeaderSearchView headerSearch;
  private final CartSummaryView cartSummary;

  public SharedPageView(AccountSessionView account, HeaderSearchView headerSearch, CartSummaryView cartSummary) {
    this.account = account == null ? AccountSessionView.anonymous() : account;
    this.headerSearch = headerSearch == null ? HeaderSearchView.empty() : headerSearch;
    this.cartSummary = cartSummary == null ? CartSummaryView.empty() : cartSummary;
  }

  public static SharedPageView anonymous() {
    return new SharedPageView(AccountSessionView.anonymous(), HeaderSearchView.empty(), CartSummaryView.empty());
  }

  public boolean isAuthenticated() {
    return account.isAuthenticated();
  }

  public AccountSessionView getAccount() {
    return account;
  }

  public AuthenticatedUserView getAuthenticatedUser() {
    return account.getAuthenticatedUser();
  }

  public HeaderSearchView getHeaderSearch() {
    return headerSearch;
  }

  public CartSummaryView getCartSummary() {
    return cartSummary;
  }

  public AccountBannerView getAccountBanner() {
    return account.getBanner();
  }

  public AccountFavoriteListView getAccountFavoriteList() {
    return account.getFavoriteList();
  }

}

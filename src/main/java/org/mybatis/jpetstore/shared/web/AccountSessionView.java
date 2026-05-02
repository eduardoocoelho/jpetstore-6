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

public class AccountSessionView {

  private final AuthenticatedUserView authenticatedUser;
  private final AccountBannerView banner;
  private final AccountFavoriteListView favoriteList;

  public AccountSessionView(AuthenticatedUserView authenticatedUser, AccountBannerView banner,
      AccountFavoriteListView favoriteList) {
    this.authenticatedUser = authenticatedUser == null ? AuthenticatedUserView.anonymous() : authenticatedUser;
    this.banner = banner == null ? AccountBannerView.disabled() : banner;
    this.favoriteList = favoriteList == null ? AccountFavoriteListView.disabled() : favoriteList;
  }

  public static AccountSessionView anonymous() {
    return new AccountSessionView(AuthenticatedUserView.anonymous(), AccountBannerView.disabled(),
        AccountFavoriteListView.disabled());
  }

  public boolean isAuthenticated() {
    return authenticatedUser.isAuthenticated();
  }

  public AuthenticatedUserView getAuthenticatedUser() {
    return authenticatedUser;
  }

  public AccountBannerView getBanner() {
    return banner;
  }

  public AccountFavoriteListView getFavoriteList() {
    return favoriteList;
  }

}

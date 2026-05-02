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

public class AuthenticatedUserView {

  private final boolean authenticated;
  private final String username;
  private final String displayName;

  public AuthenticatedUserView(boolean authenticated, String username, String displayName) {
    this.authenticated = authenticated;
    this.username = username;
    this.displayName = displayName;
  }

  public static AuthenticatedUserView anonymous() {
    return new AuthenticatedUserView(false, null, null);
  }

  public boolean isAuthenticated() {
    return authenticated;
  }

  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }

}

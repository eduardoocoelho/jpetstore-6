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
package org.mybatis.jpetstore.catalog.web;

import java.util.Locale;

import org.mybatis.jpetstore.catalog.domain.Category;

public class CategoryNavigationItem {

  private final String categoryId;
  private final String name;
  private final String smallImagePath;
  private final String iconImagePath;
  private final String sidebarDescription;

  public CategoryNavigationItem(Category category, String sidebarDescription) {
    this.categoryId = category.getCategoryId();
    this.name = category.getName();
    this.smallImagePath = "../images/sm_" + imageName(categoryId) + ".gif";
    this.iconImagePath = "../images/" + imageName(categoryId) + "_icon.gif";
    this.sidebarDescription = sidebarDescription;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public String getName() {
    return name;
  }

  public String getSmallImagePath() {
    return smallImagePath;
  }

  public String getIconImagePath() {
    return iconImagePath;
  }

  public String getSidebarDescription() {
    return sidebarDescription;
  }

  private static String imageName(String categoryId) {
    return categoryId.toLowerCase(Locale.ENGLISH);
  }

}

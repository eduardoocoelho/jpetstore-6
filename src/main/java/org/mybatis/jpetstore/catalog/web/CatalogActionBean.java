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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.SessionScope;
import net.sourceforge.stripes.integration.spring.SpringBean;

import org.mybatis.jpetstore.catalog.application.CatalogService;
import org.mybatis.jpetstore.catalog.domain.Category;
import org.mybatis.jpetstore.catalog.domain.Item;
import org.mybatis.jpetstore.catalog.domain.Product;
import org.mybatis.jpetstore.inventory.api.InventoryStatus;
import org.mybatis.jpetstore.shared.web.AbstractActionBean;

/**
 * The Class CatalogActionBean.
 *
 * @author Eduardo Macarron
 */
@SessionScope
public class CatalogActionBean extends AbstractActionBean {

  private static final long serialVersionUID = 5849523372175050635L;

  private static final String MAIN = "/WEB-INF/jsp/catalog/Main.jsp";
  private static final String VIEW_CATEGORY = "/WEB-INF/jsp/catalog/Category.jsp";
  private static final String VIEW_PRODUCT = "/WEB-INF/jsp/catalog/Product.jsp";
  private static final String VIEW_ITEM = "/WEB-INF/jsp/catalog/Item.jsp";
  private static final String SEARCH_PRODUCTS = "/WEB-INF/jsp/catalog/SearchProducts.jsp";
  private static final List<String> SIDEBAR_CATEGORY_ORDER = List.of("FISH", "DOGS", "CATS", "REPTILES", "BIRDS");
  private static final Map<String, String> SIDEBAR_CATEGORY_DESCRIPTIONS = Map.of("FISH", "Saltwater, Freshwater",
      "DOGS", "Various Breeds", "CATS", "Various Breeds, Exotic Varieties", "REPTILES", "Lizards, Turtles, Snakes",
      "BIRDS", "Exotic Varieties");
  private static final List<ImageMapAreaDefinition> MAIN_IMAGE_MAP_AREAS = List.of(
      new ImageMapAreaDefinition("BIRDS", "72,2,280,250"), new ImageMapAreaDefinition("FISH", "2,180,72,250"),
      new ImageMapAreaDefinition("DOGS", "60,250,130,320"), new ImageMapAreaDefinition("REPTILES", "140,270,210,340"),
      new ImageMapAreaDefinition("CATS", "225,240,295,310"), new ImageMapAreaDefinition("BIRDS", "280,180,350,250"));

  @SpringBean
  private transient CatalogService catalogService;

  private String keyword;

  private String categoryId;
  private Category category;
  private List<Category> categoryList;

  private String productId;
  private Product product;
  private List<Product> productList;

  private String itemId;
  private Item item;
  private InventoryStatus inventoryStatus;
  private List<Item> itemList;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public InventoryStatus getInventoryStatus() {
    return inventoryStatus;
  }

  public void setInventoryStatus(InventoryStatus inventoryStatus) {
    this.inventoryStatus = inventoryStatus;
  }

  public List<Category> getCategoryList() {
    if (categoryList == null && catalogService != null) {
      categoryList = catalogService.getCategoryList();
    }
    return categoryList;
  }

  public void setCategoryList(List<Category> categoryList) {
    this.categoryList = categoryList;
  }

  public List<Product> getProductList() {
    return productList;
  }

  public void setProductList(List<Product> productList) {
    this.productList = productList;
  }

  public List<Item> getItemList() {
    return itemList;
  }

  public void setItemList(List<Item> itemList) {
    this.itemList = itemList;
  }

  public List<CategoryNavigationItem> getQuickLinkCategories() {
    return toNavigationItems(getCategoryList());
  }

  public List<CategoryNavigationItem> getSidebarCategories() {
    List<Category> categories = getCategoryList();
    if (categories == null) {
      return List.of();
    }
    List<Category> sortedCategories = new ArrayList<>(categories);
    sortedCategories.sort(Comparator.comparingInt(category -> sidebarOrder(category.getCategoryId())));
    return toNavigationItems(sortedCategories);
  }

  public List<CategoryImageMapArea> getMainImageMapAreas() {
    List<Category> categories = getCategoryList();
    if (categories == null) {
      return List.of();
    }
    return MAIN_IMAGE_MAP_AREAS.stream().map(area -> toImageMapArea(area, categories)).toList();
  }

  @DefaultHandler
  public ForwardResolution viewMain() {
    return new ForwardResolution(MAIN);
  }

  /**
   * View category.
   *
   * @return the forward resolution
   */
  public ForwardResolution viewCategory() {
    if (categoryId != null) {
      productList = catalogService.getProductListByCategory(categoryId);
      category = catalogService.getCategory(categoryId);
    }
    return new ForwardResolution(VIEW_CATEGORY);
  }

  /**
   * View product.
   *
   * @return the forward resolution
   */
  public ForwardResolution viewProduct() {
    if (productId != null) {
      itemList = catalogService.getItemListByProduct(productId);
      product = catalogService.getProduct(productId);
    }
    return new ForwardResolution(VIEW_PRODUCT);
  }

  /**
   * View item.
   *
   * @return the forward resolution
   */
  public ForwardResolution viewItem() {
    item = catalogService.getItem(itemId);
    inventoryStatus = catalogService.getInventoryStatus(itemId);
    product = item.getProduct();
    return new ForwardResolution(VIEW_ITEM);
  }

  /**
   * Search products.
   *
   * @return the forward resolution
   */
  public ForwardResolution searchProducts() {
    if (keyword == null || keyword.length() < 1) {
      setMessage("Please enter a keyword to search for, then press the search button.");
      return new ForwardResolution(ERROR);
    } else {
      productList = catalogService.searchProductList(keyword.toLowerCase());
      return new ForwardResolution(SEARCH_PRODUCTS);
    }
  }

  /**
   * Clear.
   */
  public void clear() {
    keyword = null;

    categoryId = null;
    category = null;
    categoryList = null;

    productId = null;
    product = null;
    productList = null;

    itemId = null;
    item = null;
    inventoryStatus = null;
    itemList = null;
  }

  private static List<CategoryNavigationItem> toNavigationItems(List<Category> categories) {
    if (categories == null) {
      return List.of();
    }
    return categories.stream().map(category -> new CategoryNavigationItem(category,
        SIDEBAR_CATEGORY_DESCRIPTIONS.getOrDefault(category.getCategoryId(), category.getName()))).toList();
  }

  private static int sidebarOrder(String categoryId) {
    int index = SIDEBAR_CATEGORY_ORDER.indexOf(categoryId);
    return index < 0 ? SIDEBAR_CATEGORY_ORDER.size() : index;
  }

  private static CategoryImageMapArea toImageMapArea(ImageMapAreaDefinition area, List<Category> categories) {
    return categories.stream().filter(category -> area.categoryId().equals(category.getCategoryId())).findFirst()
        .map(category -> new CategoryImageMapArea(category, area.coordinates()))
        .orElseThrow(() -> new IllegalStateException("Category " + area.categoryId() + " is missing."));
  }

  private record ImageMapAreaDefinition(String categoryId, String coordinates) {
  }

}

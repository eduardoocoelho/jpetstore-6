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
package org.mybatis.jpetstore.service;

import java.util.ArrayList;
import java.util.List;

import org.mybatis.jpetstore.catalog.api.CatalogQueryService;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.catalog.api.ProductSummary;
import org.mybatis.jpetstore.domain.Category;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.inventory.api.InventoryQueryService;
import org.mybatis.jpetstore.inventory.api.InventoryStatus;
import org.mybatis.jpetstore.inventory.persistence.InventoryMapper;
import org.mybatis.jpetstore.mapper.CategoryMapper;
import org.mybatis.jpetstore.mapper.ItemMapper;
import org.mybatis.jpetstore.mapper.ProductMapper;
import org.springframework.stereotype.Service;

/**
 * The Class CatalogService.
 *
 * @author Eduardo Macarron
 */
@Service
public class CatalogService implements CatalogQueryService, InventoryQueryService {

  private final CategoryMapper categoryMapper;
  private final ItemMapper itemMapper;
  private final InventoryMapper inventoryMapper;
  private final ProductMapper productMapper;

  public CatalogService(CategoryMapper categoryMapper, ItemMapper itemMapper, InventoryMapper inventoryMapper,
      ProductMapper productMapper) {
    this.categoryMapper = categoryMapper;
    this.itemMapper = itemMapper;
    this.inventoryMapper = inventoryMapper;
    this.productMapper = productMapper;
  }

  @Override
  public List<Category> getCategoryList() {
    return categoryMapper.getCategoryList();
  }

  @Override
  public Category getCategory(String categoryId) {
    return categoryMapper.getCategory(categoryId);
  }

  @Override
  public Product getProduct(String productId) {
    return productMapper.getProduct(productId);
  }

  @Override
  public ProductSummary getProductSummary(String productId) {
    return toProductSummary(getProduct(productId));
  }

  @Override
  public List<Product> getProductListByCategory(String categoryId) {
    return productMapper.getProductListByCategory(categoryId);
  }

  @Override
  public List<ProductSummary> getProductSummariesByCategory(String categoryId) {
    return getProductListByCategory(categoryId).stream().map(CatalogService::toProductSummary).toList();
  }

  /**
   * Search product list.
   *
   * @param keywords
   *          the keywords
   *
   * @return the list
   */
  @Override
  public List<Product> searchProductList(String keywords) {
    List<Product> products = new ArrayList<>();
    for (String keyword : keywords.split("\\s+")) {
      products.addAll(productMapper.searchProductList("%" + keyword.toLowerCase() + "%"));
    }
    return products;
  }

  @Override
  public List<Item> getItemListByProduct(String productId) {
    return itemMapper.getItemListByProduct(productId);
  }

  @Override
  public Item getItem(String itemId) {
    return itemMapper.getItem(itemId);
  }

  @Override
  public ItemSnapshot getItemSnapshot(String itemId) {
    return toItemSnapshot(getItem(itemId));
  }

  @Override
  public boolean isItemInStock(String itemId) {
    return inventoryMapper.getInventoryQuantity(itemId) > 0;
  }

  @Override
  public InventoryStatus getInventoryStatus(String itemId) {
    int quantity = inventoryMapper.getInventoryQuantity(itemId);
    return new InventoryStatus(itemId, quantity, quantity > 0);
  }

  private static ProductSummary toProductSummary(Product product) {
    if (product == null) {
      return null;
    }
    return new ProductSummary(product.getProductId(), product.getCategoryId(), product.getName(),
        product.getDescription());
  }

  private static ItemSnapshot toItemSnapshot(Item item) {
    if (item == null) {
      return null;
    }
    Product product = item.getProduct();
    return new ItemSnapshot(item.getItemId(), product == null ? null : product.getProductId(),
        toProductSummary(product), item.getListPrice(), item.getStatus(), item.getAttribute1(), item.getAttribute2(),
        item.getAttribute3(), item.getAttribute4(), item.getAttribute5());
  }
}

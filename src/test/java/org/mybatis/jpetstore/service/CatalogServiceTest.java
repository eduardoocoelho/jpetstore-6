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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

/**
 * @author Eduardo Macarron
 */
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

  @Mock(lenient = true)
  private ProductMapper productMapper;
  @Mock
  private CategoryMapper categoryMapper;
  @Mock
  private ItemMapper itemMapper;
  @Mock
  private InventoryMapper inventoryMapper;

  @InjectMocks
  private CatalogService catalogService;

  @Test
  void shouldImplementCatalogAndInventoryQueryServiceApis() {
    assertThat(catalogService).isInstanceOf(CatalogQueryService.class).isInstanceOf(InventoryQueryService.class);
  }

  @Test
  void shouldCallTheSearchMapperTwice() {
    // given
    String keywords = "a b";
    List<Product> l1 = new ArrayList<>();
    l1.add(new Product());
    List<Product> l2 = new ArrayList<>();
    l2.add(new Product());

    // when
    when(productMapper.searchProductList("%a%")).thenReturn(l1);
    when(productMapper.searchProductList("%b%")).thenReturn(l2);
    List<Product> r = catalogService.searchProductList(keywords);

    // then
    assertThat(r).hasSize(2);
    assertThat(r.get(0)).isSameAs(l1.get(0));
    assertThat(r.get(1)).isSameAs(l2.get(0));
  }

  @Test
  void shouldReturnCategoryList() {
    // given
    List<Category> expectedCategories = new ArrayList<>();

    // when
    when(categoryMapper.getCategoryList()).thenReturn(expectedCategories);
    List<Category> categories = catalogService.getCategoryList();

    // then
    assertThat(categories).isSameAs(expectedCategories);
  }

  @Test
  void shouldReturnCategory() {

    // given
    String categoryId = "C01";
    Category expectedCategory = new Category();

    // when
    when(categoryMapper.getCategory(categoryId)).thenReturn(expectedCategory);
    Category category = catalogService.getCategory(categoryId);

    // then
    assertThat(category).isSameAs(expectedCategory);

  }

  @Test
  void shouldReturnProduct() {

    // given
    String productId = "P01";
    Product expectedProduct = new Product();

    // when
    when(productMapper.getProduct(productId)).thenReturn(expectedProduct);
    Product product = catalogService.getProduct(productId);

    // then
    assertThat(product).isSameAs(expectedProduct);

  }

  @Test
  void shouldMapProductToProductSummary() {
    // given
    String productId = "P01";
    Product product = new Product();
    product.setProductId(productId);
    product.setCategoryId("C01");
    product.setName("Angelfish");
    product.setDescription("Fresh Water fish from China");

    // when
    when(productMapper.getProduct(productId)).thenReturn(product);
    ProductSummary productSummary = catalogService.getProductSummary(productId);

    // then
    assertThat(productSummary.productId()).isEqualTo(productId);
    assertThat(productSummary.categoryId()).isEqualTo("C01");
    assertThat(productSummary.name()).isEqualTo("Angelfish");
    assertThat(productSummary.description()).isEqualTo("Fresh Water fish from China");
  }

  @Test
  void shouldReturnProductList() {
    // given
    String categoryId = "C01";
    List<Product> expectedProducts = new ArrayList<>();

    // when
    when(productMapper.getProductListByCategory(categoryId)).thenReturn(expectedProducts);
    List<Product> products = catalogService.getProductListByCategory(categoryId);

    // then
    assertThat(products).isSameAs(expectedProducts);

  }

  @Test
  void shouldMapProductsByCategoryToProductSummaries() {
    // given
    String categoryId = "C01";
    Product product = new Product();
    product.setProductId("P01");
    product.setCategoryId(categoryId);
    product.setName("Angelfish");
    product.setDescription("Fresh Water fish from China");

    // when
    when(productMapper.getProductListByCategory(categoryId)).thenReturn(List.of(product));
    List<ProductSummary> productSummaries = catalogService.getProductSummariesByCategory(categoryId);

    // then
    assertThat(productSummaries).hasSize(1);
    assertThat(productSummaries.get(0).productId()).isEqualTo("P01");
    assertThat(productSummaries.get(0).categoryId()).isEqualTo(categoryId);
    assertThat(productSummaries.get(0).name()).isEqualTo("Angelfish");
  }

  @Test
  void shouldReturnItemList() {
    // given
    String productId = "P01";
    List<Item> expectedItems = new ArrayList<>();

    // when
    when(itemMapper.getItemListByProduct(productId)).thenReturn(expectedItems);
    List<Item> items = catalogService.getItemListByProduct(productId);

    // then
    assertThat(items).isSameAs(expectedItems);

  }

  @Test
  void shouldReturnItem() {

    // given
    String itemCode = "I01";
    Item expectedItem = new Item();

    // when
    when(itemMapper.getItem(itemCode)).thenReturn(expectedItem);
    Item item = catalogService.getItem(itemCode);

    // then
    assertThat(item).isSameAs(expectedItem);

  }

  @Test
  void shouldMapItemToItemSnapshotWithoutInventoryQuantity() {
    // given
    String itemCode = "I01";
    Product product = new Product();
    product.setProductId("P01");
    product.setCategoryId("C01");
    product.setName("Angelfish");
    product.setDescription("Fresh Water fish from China");
    Item item = new Item();
    item.setItemId(itemCode);
    item.setProduct(product);
    item.setListPrice(new java.math.BigDecimal("16.50"));
    item.setStatus("P");
    item.setAttribute1("Large");
    item.setAttribute2("Male");
    item.setQuantity(7);

    // when
    when(itemMapper.getItem(itemCode)).thenReturn(item);
    ItemSnapshot itemSnapshot = catalogService.getItemSnapshot(itemCode);

    // then
    assertThat(itemSnapshot.itemId()).isEqualTo(itemCode);
    assertThat(itemSnapshot.productId()).isEqualTo("P01");
    assertThat(itemSnapshot.product().name()).isEqualTo("Angelfish");
    assertThat(itemSnapshot.listPrice()).isEqualTo(new java.math.BigDecimal("16.50"));
    assertThat(itemSnapshot.status()).isEqualTo("P");
    assertThat(itemSnapshot.attribute1()).isEqualTo("Large");
    assertThat(itemSnapshot.attribute2()).isEqualTo("Male");
  }

  @Test
  void shouldReturnTrueWhenExistStock() {

    // given
    String itemCode = "I01";

    // when
    when(inventoryMapper.getInventoryQuantity(itemCode)).thenReturn(1);
    boolean result = catalogService.isItemInStock(itemCode);

    // then
    assertThat(result).isTrue();

  }

  @Test
  void shouldReturnFalseWhenNotExistStock() {

    // given
    String itemCode = "I01";

    // when
    when(inventoryMapper.getInventoryQuantity(itemCode)).thenReturn(0);
    boolean result = catalogService.isItemInStock(itemCode);

    // then
    assertThat(result).isFalse();

  }

  @Test
  void shouldMapInventoryQuantityToInventoryStatus() {
    // given
    String itemCode = "I01";

    // when
    when(inventoryMapper.getInventoryQuantity(itemCode)).thenReturn(3);
    InventoryStatus inventoryStatus = catalogService.getInventoryStatus(itemCode);

    // then
    assertThat(inventoryStatus.itemId()).isEqualTo(itemCode);
    assertThat(inventoryStatus.quantity()).isEqualTo(3);
    assertThat(inventoryStatus.inStock()).isTrue();
  }

}

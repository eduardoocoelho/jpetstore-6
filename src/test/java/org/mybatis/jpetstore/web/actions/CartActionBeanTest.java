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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.action.Resolution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.jpetstore.cart.application.CartService;
import org.mybatis.jpetstore.catalog.domain.Item;
import org.mybatis.jpetstore.domain.Cart;
import org.springframework.test.util.ReflectionTestUtils;

class CartActionBeanTest {

  private CartActionBean cartActionBean;
  private ActionBeanContext mockContext;
  private CartService cartService;

  @BeforeEach
  void setUp() {
    cartActionBean = new CartActionBean();
    cartActionBean.setCart(new Cart());
    cartService = mock(CartService.class);
    ReflectionTestUtils.setField(cartActionBean, "cartService", cartService);

    // Mock ActionBeanContext to avoid NPE in setMessage()
    mockContext = mock(ActionBeanContext.class);
    when(mockContext.getMessages()).thenReturn(new ArrayList<Message>());
    cartActionBean.setContext(mockContext);
  }

  @Test
  void constructorOutputNotNull() {
    final CartActionBean actual = new CartActionBean();

    assertThat(actual).isNotNull();
    assertThat(actual.getCart()).isNotNull();
    assertThat(actual.getContext()).isNull();
  }

  @Test
  void getCartOutputNotNull() {
    final CartActionBean bean = new CartActionBean();

    assertThat(bean.getCart()).isNotNull();
  }

  @Test
  void addItemToCart_WithNullWorkingItemId_ShouldReturnError() {
    cartActionBean.setWorkingItemId(null);

    Resolution resolution = cartActionBean.addItemToCart();

    assertThat(resolution).isNotNull();
    assertThat(resolution.toString()).contains("Error.jsp");
  }

  @Test
  void addItemToCart_WithEmptyWorkingItemId_ShouldReturnError() {
    cartActionBean.setWorkingItemId("");

    Resolution resolution = cartActionBean.addItemToCart();

    assertThat(resolution).isNotNull();
    assertThat(resolution.toString()).contains("Error.jsp");
  }

  @Test
  void addItemToCart_WithBlankWorkingItemId_ShouldReturnError() {
    cartActionBean.setWorkingItemId("   ");

    Resolution resolution = cartActionBean.addItemToCart();

    assertThat(resolution).isNotNull();
    assertThat(resolution.toString()).contains("Error.jsp");
  }

  @Test
  void addItemToCartWhenWorkingItemIdIsValidDelegatesToCartService() {
    cartActionBean.setWorkingItemId("EST-1");

    Resolution resolution = cartActionBean.addItemToCart();

    assertThat(resolution.toString()).contains("Cart.jsp");
    verify(cartService).addItem(same(cartActionBean.getCart()), eq("EST-1"));
  }

  @Test
  void addItemToCartWhenItemIsAlreadyPresentStillDelegatesToCartService() {
    Item item = item("EST-1", "16.50");
    cartActionBean.getCart().addItem(item, true);
    cartActionBean.setWorkingItemId("EST-1");

    Resolution resolution = cartActionBean.addItemToCart();

    assertThat(resolution.toString()).contains("Cart.jsp");
    verify(cartService).addItem(same(cartActionBean.getCart()), eq("EST-1"));
  }

  @Test
  void removeItemFromCart_WithNullWorkingItemId_ShouldReturnError() {
    cartActionBean.setWorkingItemId(null);

    Resolution resolution = cartActionBean.removeItemFromCart();

    assertThat(resolution).isNotNull();
    assertThat(resolution.toString()).contains("Error.jsp");
  }

  @Test
  void removeItemFromCart_WithEmptyWorkingItemId_ShouldReturnError() {
    cartActionBean.setWorkingItemId("");

    Resolution resolution = cartActionBean.removeItemFromCart();

    assertThat(resolution).isNotNull();
    assertThat(resolution.toString()).contains("Error.jsp");
  }

  @Test
  void removeItemFromCart_WithBlankWorkingItemId_ShouldReturnError() {
    cartActionBean.setWorkingItemId("   ");

    Resolution resolution = cartActionBean.removeItemFromCart();

    assertThat(resolution).isNotNull();
    assertThat(resolution.toString()).contains("Error.jsp");
  }

  @Test
  void removeItemFromCart_WithNonExistentItem_ShouldReturnError() {
    cartActionBean.setWorkingItemId("NON_EXISTENT_ITEM");

    Resolution resolution = cartActionBean.removeItemFromCart();

    assertThat(resolution).isNotNull();
    assertThat(resolution.toString()).contains("Error.jsp");
  }

  @Test
  void removeItemFromCartWhenItemExistsRemovesItAndReturnsCartView() {
    Item item = item("EST-1", "16.50");
    cartActionBean.getCart().addItem(item, true);
    cartActionBean.setWorkingItemId("EST-1");

    Resolution resolution = cartActionBean.removeItemFromCart();

    assertThat(resolution.toString()).contains("Cart.jsp");
    assertThat(cartActionBean.getCart().containsItemId("EST-1")).isFalse();
    assertThat(cartActionBean.getCart().getNumberOfItems()).isZero();
  }

  @Test
  void updateCartQuantitiesAppliesNumericValuesAndRemovesItemsBelowOneFromVisibleList() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    Item keptItem = item("EST-1", "16.50");
    Item removedItem = item("EST-2", "12.00");
    cartActionBean.getCart().addItem(keptItem, true);
    cartActionBean.getCart().addItem(removedItem, true);
    when(mockContext.getRequest()).thenReturn(request);
    when(request.getParameter("EST-1")).thenReturn("3");
    when(request.getParameter("EST-2")).thenReturn("0");

    Resolution resolution = cartActionBean.updateCartQuantities();

    assertThat(resolution.toString()).contains("Cart.jsp");
    assertThat(cartActionBean.getCart().containsItemId("EST-1")).isTrue();
    assertThat(cartActionBean.getCart().containsItemId("EST-2")).isTrue();
    assertThat(cartActionBean.getCart().getNumberOfItems()).isEqualTo(1);
    assertThat(cartActionBean.getCart().getCartItemList()).extracting(cartItem -> cartItem.getItem().getItemId())
        .containsExactly("EST-1");
    assertThat(cartActionBean.getCart().getCartItemList().get(0).getQuantity()).isEqualTo(3);
  }

  @Test
  void clearShouldResetCartAndWorkingItemId() {
    cartActionBean.setWorkingItemId("EST-1");

    cartActionBean.clear();

    assertThat(cartActionBean.getCart()).isNotNull();
    assertThat(cartActionBean.getCart().getNumberOfItems()).isZero();
  }

  private static Item item(String itemId, String price) {
    Item item = new Item();
    item.setItemId(itemId);
    item.setListPrice(new BigDecimal(price));
    return item;
  }
}

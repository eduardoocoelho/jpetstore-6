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
package org.mybatis.jpetstore.cart.web;

import java.math.BigDecimal;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SessionScope;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.integration.spring.SpringBean;

import org.mybatis.jpetstore.cart.api.CartQueryService;
import org.mybatis.jpetstore.cart.api.CartSnapshot;
import org.mybatis.jpetstore.cart.application.CartService;
import org.mybatis.jpetstore.cart.domain.Cart;
import org.mybatis.jpetstore.cart.domain.CartItem;
import org.mybatis.jpetstore.catalog.api.ItemSnapshot;
import org.mybatis.jpetstore.shared.web.AbstractActionBean;
import org.mybatis.jpetstore.shared.web.SessionCart;

/**
 * The Class CartActionBean.
 *
 * @author Eduardo Macarron
 */
@SessionScope
@UrlBinding("/actions/Cart.action")
public class CartActionBean extends AbstractActionBean implements SessionCart {

  private static final long serialVersionUID = -4038684592582714235L;

  private static final String VIEW_CART = "/WEB-INF/jsp/cart/Cart.jsp";
  private static final String CHECK_OUT = "/WEB-INF/jsp/cart/Checkout.jsp";
  private static final CartQueryService CART_QUERY_SERVICE = new ActionCartQueryService();

  @SpringBean
  private transient CartService cartService;

  private Cart cart = new Cart();
  private String workingItemId;

  public Cart getCart() {
    return cart;
  }

  public void setCart(Cart cart) {
    this.cart = cart;
  }

  public void setWorkingItemId(String workingItemId) {
    this.workingItemId = workingItemId;
  }

  /**
   * Adds the item to cart.
   *
   * @return the resolution
   */
  public Resolution addItemToCart() {

    if (workingItemId == null || workingItemId.trim().isEmpty()) {
      setMessage("Invalid item ID: cannot add item to cart.");
      return new ForwardResolution(ERROR);
    }

    cartService.addItem(cart, workingItemId);

    return new ForwardResolution(VIEW_CART);
  }

  /**
   * Removes the item from cart.
   *
   * @return the resolution
   */
  public Resolution removeItemFromCart() {

    if (workingItemId == null || workingItemId.trim().isEmpty()) {
      setMessage("Invalid item ID: cannot remove item from cart.");
      return new ForwardResolution(ERROR);
    }

    ItemSnapshot item = cart.removeItemById(workingItemId);

    if (item == null) {
      setMessage("Attempted to remove null CartItem from Cart.");
      return new ForwardResolution(ERROR);
    } else {
      return new ForwardResolution(VIEW_CART);
    }
  }

  /**
   * Update cart quantities.
   *
   * @return the resolution
   */
  public Resolution updateCartQuantities() {
    HttpServletRequest request = context.getRequest();

    Iterator<CartItem> cartItems = getCart().getAllCartItems();
    while (cartItems.hasNext()) {
      CartItem cartItem = cartItems.next();
      String itemId = cartItem.getItem().itemId();
      try {
        int quantity = Integer.parseInt(request.getParameter(itemId));
        getCart().setQuantityByItemId(itemId, quantity);
        if (quantity < 1) {
          cartItems.remove();
        }
      } catch (NumberFormatException e) {
        // ignore invalid numeric input on purpose
      }
    }

    return new ForwardResolution(VIEW_CART);
  }

  public ForwardResolution viewCart() {
    return new ForwardResolution(VIEW_CART);
  }

  public ForwardResolution checkOut() {
    return new ForwardResolution(CHECK_OUT);
  }

  @Override
  public CartSnapshot getCurrentCartSnapshot() {
    return cart == null ? null : CART_QUERY_SERVICE.getCartSnapshot(cart);
  }

  @Override
  public void clearCart() {
    clear();
  }

  public void clear() {
    cart = new Cart();
    workingItemId = null;
  }

  private static class ActionCartQueryService implements CartQueryService {

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

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
package org.mybatis.jpetstore.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mybatis.jpetstore.account.api.AccountQueryService;
import org.mybatis.jpetstore.account.api.CustomerProfile;
import org.mybatis.jpetstore.account.domain.Account;
import org.mybatis.jpetstore.account.persistence.AccountMapper;

/**
 * @author Eduardo Macarron
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock
  private AccountMapper accountMapper;

  @InjectMocks
  private AccountService accountService;

  @Test
  void shouldImplementAccountQueryServiceApi() {
    assertThat(accountService).isInstanceOf(AccountQueryService.class);
  }

  @Test
  void shouldCallTheMapperToInsertAnAccount() {
    // given
    Account account = new Account();

    // when
    accountService.insertAccount(account);

    // then
    verify(accountMapper).insertAccount(eq(account));
    verify(accountMapper).insertProfile(eq(account));
    verify(accountMapper).insertSignon(eq(account));
  }

  @Test
  void shouldCallTheMapperToUpdateAnAccount() {
    // given
    Account account = new Account();
    account.setPassword("foo");

    // when
    accountService.updateAccount(account);

    // then
    verify(accountMapper).updateAccount(eq(account));
    verify(accountMapper).updateProfile(eq(account));
    verify(accountMapper).updateSignon(eq(account));
  }

  @Test
  void shouldCallTheMapperToGetAccountAnUsername() {
    // given
    String username = "bar";
    Account expectedAccount = new Account();
    when(accountMapper.getAccountByUsername(username)).thenReturn(expectedAccount);

    // when
    Account account = accountService.getAccount(username);

    // then
    assertThat(account).isSameAs(expectedAccount);
  }

  @Test
  void shouldCallTheMapperToGetAccountAnUsernameAndPassword() {
    // given
    String username = "bar";
    String password = "foo";

    // when
    Account expectedAccount = new Account();
    when(accountMapper.getAccountByUsernameAndPassword(username, password)).thenReturn(expectedAccount);
    Account account = accountService.getAccount(username, password);

    // then
    assertThat(account).isSameAs(expectedAccount);
  }

  @Test
  void shouldMapAccountToCustomerProfile() {
    // given
    Account account = new Account();
    account.setUsername("j2ee");
    account.setEmail("j2ee@example.com");
    account.setFirstName("Jane");
    account.setLastName("Doe");
    account.setAddress1("1 Main Street");
    account.setAddress2("Apt 2");
    account.setCity("Denver");
    account.setState("CO");
    account.setZip("80202");
    account.setCountry("USA");
    account.setPhone("555-0100");
    account.setFavouriteCategoryId("DOGS");
    account.setLanguagePreference("english");
    account.setPassword("secret");
    when(accountMapper.getAccountByUsername("j2ee")).thenReturn(account);

    // when
    CustomerProfile customerProfile = accountService.getCustomerProfile("j2ee");

    // then
    assertThat(customerProfile.username()).isEqualTo("j2ee");
    assertThat(customerProfile.email()).isEqualTo("j2ee@example.com");
    assertThat(customerProfile.firstName()).isEqualTo("Jane");
    assertThat(customerProfile.lastName()).isEqualTo("Doe");
    assertThat(customerProfile.address1()).isEqualTo("1 Main Street");
    assertThat(customerProfile.address2()).isEqualTo("Apt 2");
    assertThat(customerProfile.city()).isEqualTo("Denver");
    assertThat(customerProfile.state()).isEqualTo("CO");
    assertThat(customerProfile.zip()).isEqualTo("80202");
    assertThat(customerProfile.country()).isEqualTo("USA");
    assertThat(customerProfile.phone()).isEqualTo("555-0100");
    assertThat(customerProfile.favouriteCategoryId()).isEqualTo("DOGS");
    assertThat(customerProfile.languagePreference()).isEqualTo("english");
  }

}

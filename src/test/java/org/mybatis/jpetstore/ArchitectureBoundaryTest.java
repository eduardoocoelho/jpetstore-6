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
package org.mybatis.jpetstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {

  private static final Path MAIN_SOURCES = Path.of("src/main/java/org/mybatis/jpetstore");

  @Test
  void modulesRespectDeclaredDependencyBoundaries() throws IOException {
    List<Rule> rules = List.of(
        new Rule("account must not depend on cart, order, or inventory persistence", pathStartsWith("account"),
            List.of("org.mybatis.jpetstore.cart.", "org.mybatis.jpetstore.order.",
                "org.mybatis.jpetstore.inventory.persistence.")),
        new Rule("catalog must not depend on cart, order, inventory persistence, or inventory mutation",
            pathStartsWith("catalog"),
            List.of("org.mybatis.jpetstore.cart.", "org.mybatis.jpetstore.order.",
                "org.mybatis.jpetstore.inventory.persistence.",
                "org.mybatis.jpetstore.inventory.api.InventoryReservationService")),
        new Rule("cart must not depend on order", pathStartsWith("cart"), List.of("org.mybatis.jpetstore.order.")),
        new Rule("order must not depend on account web, cart web, or catalog persistence", pathStartsWith("order"),
            List.of("org.mybatis.jpetstore.account.web.", "org.mybatis.jpetstore.cart.web.",
                "org.mybatis.jpetstore.catalog.persistence.")),
        new Rule("shared must not depend on business modules", pathStartsWith("shared"),
            List.of("org.mybatis.jpetstore.account.", "org.mybatis.jpetstore.catalog.",
                "org.mybatis.jpetstore.inventory.", "org.mybatis.jpetstore.cart.", "org.mybatis.jpetstore.order.")));

    assertThat(violationsFor(rules)).isEmpty();
  }

  @Test
  void domainPackagesStayFrameworkNeutral() throws IOException {
    Rule rule = new Rule("domain packages must not depend on web, Spring, or MyBatis frameworks",
        path -> path.toString().contains("/domain/"), List.of("net.sourceforge.stripes", "javax.servlet",
            "jakarta.servlet", "org.springframework", "org.apache.ibatis"));

    assertThat(violationsFor(List.of(rule))).isEmpty();
  }

  private static Predicate<Path> pathStartsWith(String module) {
    return path -> MAIN_SOURCES.relativize(path).startsWith(module);
  }

  private static List<String> violationsFor(List<Rule> rules) throws IOException {
    try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
      return sources.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
          .flatMap(path -> violationsIn(path, rules).stream()).toList();
    }
  }

  private static List<String> violationsIn(Path path, List<Rule> rules) {
    try {
      List<String> lines = Files.readAllLines(path);
      return rules.stream().filter(rule -> rule.appliesTo().test(path))
          .flatMap(rule -> violationsIn(path, lines, rule).stream()).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<String> violationsIn(Path path, List<String> lines, Rule rule) {
    return lines.stream().map(String::trim).filter(line -> !line.startsWith("package "))
        .flatMap(line -> rule.forbiddenReferences().stream().filter(line::contains)
            .map(reference -> path + ": " + rule.description() + " references " + reference))
        .toList();
  }

  private record Rule(String description, Predicate<Path> appliesTo, List<String> forbiddenReferences) {
  }
}

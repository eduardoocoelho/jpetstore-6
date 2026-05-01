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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class DomainFrameworkDependencyTest {

  private static final Path MAIN_SOURCES = Path.of("src/main/java/org/mybatis/jpetstore");

  private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of("net.sourceforge.stripes", "javax.servlet",
      "jakarta.servlet", "org.springframework", "org.apache.ibatis");

  @Test
  void domainSourcesDoNotImportWebOrPersistenceFrameworks() throws IOException {
    List<String> violations;
    try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
      violations = sources.filter(Files::isRegularFile).filter(path -> path.toString().contains("/domain/"))
          .flatMap(path -> importViolations(path).stream()).toList();
    }

    assertThat(violations).isEmpty();
  }

  private static List<String> importViolations(Path path) {
    try {
      return Files.readAllLines(path).stream().map(String::trim).filter(line -> line.startsWith("import "))
          .filter(line -> FORBIDDEN_IMPORT_PREFIXES.stream().anyMatch(line::contains)).map(line -> path + ": " + line)
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

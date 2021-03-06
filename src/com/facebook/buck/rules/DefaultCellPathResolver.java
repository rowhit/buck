/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.rules;

import com.facebook.buck.config.Config;
import com.facebook.buck.io.MorePaths;
import com.facebook.buck.log.Logger;
import com.facebook.buck.util.HumanReadableException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public class DefaultCellPathResolver implements CellPathResolver {
  private static final Logger LOG = Logger.get(DefaultCellPathResolver.class);

  public static final String REPOSITORIES_SECTION = "repositories";

  private final Path root;
  private final ImmutableMap<String, Path> cellPaths;
  private final ImmutableMap<Path, String> canonicalNames;

  public DefaultCellPathResolver(Path root, ImmutableMap<String, Path> cellPaths) {
    this.root = root;
    this.cellPaths = cellPaths;
    this.canonicalNames =
        cellPaths
            .entrySet()
            .stream()
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toMap(
                        Map.Entry::getValue,
                        Map.Entry::getKey,
                        BinaryOperator.minBy(Comparator.<String>naturalOrder())),
                    ImmutableMap::copyOf));
  }

  public DefaultCellPathResolver(Path root, Config config) {
    this(root, getCellPathsFromConfigRepositoriesSection(root, config.get(REPOSITORIES_SECTION)));
  }

  static ImmutableMap<String, Path> getCellPathsFromConfigRepositoriesSection(
      Path root, ImmutableMap<String, String> repositoriesSection) {
    return ImmutableMap.copyOf(
        Maps.transformValues(
            repositoriesSection,
            input ->
                root.resolve(MorePaths.expandHomeDir(root.getFileSystem().getPath(input)))
                    .normalize()));
  }

  /**
   * The root cell is required to declare all {@link Cell}s it and its dependants can reach
   *
   * @return Map of cell name to path.
   */
  public ImmutableMap<RelativeCellName, Path> getPathMapping() {
    ImmutableMap.Builder<RelativeCellName, Path> builder = ImmutableMap.builder();
    builder.put(RelativeCellName.of(ImmutableList.of()), root);
    HashSet<Path> seenPaths = new HashSet<>();
    seenPaths.add(root);

    ImmutableSortedSet<String> sortedCellNames =
        ImmutableSortedSet.<String>naturalOrder().addAll(cellPaths.keySet()).build();
    for (String cellName : sortedCellNames) {
      Path cellRoot = Preconditions.checkNotNull(getCellPath(cellName));
      try {
        cellRoot = cellRoot.toRealPath().normalize();
      } catch (IOException e) {
        LOG.warn("cellroot [" + cellRoot + "] does not exist in filesystem");
      }
      if (seenPaths.contains(cellRoot)) {
        continue;
      }
      builder.put(RelativeCellName.of(ImmutableList.of(cellName)), cellRoot);
      seenPaths.add(cellRoot);
    }
    return builder.build();
  }

  public Path getRoot() {
    return root;
  }

  private Path getCellPath(String cellName) {
    Path path = cellPaths.get(cellName);
    if (path == null) {
      throw new HumanReadableException(
          "In cell rooted at %s: cell named '%s' is not defined.", root, cellName);
    }
    return path;
  }

  @Override
  public Path getCellPath(Optional<String> cellName) {
    if (!cellName.isPresent()) {
      return root;
    }
    return getCellPath(cellName.get());
  }

  @Override
  public ImmutableMap<String, Path> getCellPaths() {
    return cellPaths;
  }

  @Override
  public Optional<String> getCanonicalCellName(Path cellPath) {
    if (cellPath.equals(root)) {
      return Optional.empty();
    } else {
      String name = canonicalNames.get(cellPath);
      if (name == null) {
        throw new IllegalArgumentException("Unknown cell path: " + cellPath);
      }
      return Optional.of(name);
    }
  }
}

/*
 * Copyright 2013-present Facebook, Inc.
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

package com.facebook.buck.apple;

import com.facebook.buck.cpp.AbstractNativeBuildRule;
import com.facebook.buck.cpp.CompilerStep;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.step.Step;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class IosTest extends AbstractNativeBuildRule {
  private final Path infoPlist;
  private final ImmutableSet<XcodeRuleConfiguration> configurations;
  private final ImmutableList<GroupedSource> srcs;
  private final ImmutableMap<SourcePath, String> perFileFlags;
  private final ImmutableSortedSet<String> frameworks;
  private final ImmutableSortedSet<BuildRule> sourceUnderTest;
  private final IosTestType testType;

  public IosTest(
      BuildRuleParams params,
      IosTestDescription.Arg arg,
      TargetSources targetSources) {
    super(
        params,
        targetSources.srcPaths,
        targetSources.headerPaths,
        targetSources.perFileFlags);
    infoPlist = Preconditions.checkNotNull(arg.infoPlist);
    configurations = XcodeRuleConfiguration.fromRawJsonStructure(arg.configs);
    frameworks = Preconditions.checkNotNull(arg.frameworks);
    sourceUnderTest = Preconditions.checkNotNull(arg.sourceUnderTest);
    Optional<String> argTestType = Preconditions.checkNotNull(arg.testType);
    testType = IosTestType.fromString(argTestType.or("octest"));
    srcs = Preconditions.checkNotNull(targetSources.srcs);
    perFileFlags = Preconditions.checkNotNull(targetSources.perFileFlags);
  }

  public Path getInfoPlist() {
    return infoPlist;
  }

  public ImmutableSet<XcodeRuleConfiguration> getConfigurations() {
    return configurations;
  }

  public ImmutableList<GroupedSource> getSrcs() {
    return srcs;
  }

  public ImmutableMap<SourcePath, String> getPerFileFlags() {
    return perFileFlags;
  }

  public ImmutableSortedSet<String> getFrameworks() {
    return frameworks;
  }

  public ImmutableSortedSet<BuildRule> getSourceUnderTest() {
    return sourceUnderTest;
  }

  public IosTestType getTestType() {
    return testType;
  }

  @Override
  protected String getCompiler() {
    return "clang";
  }

  @Override
  protected List<Step> getFinalBuildSteps(ImmutableSortedSet<Path> files, Path outputFile) {
    if (files.isEmpty()) {
      return ImmutableList.of();
    } else {
      // TODO(user): This needs to create a dylib, not a static library.
      return ImmutableList.<Step>of(
          new CompilerStep(
              /* compiler */ getCompiler(),
              /* shouldLink */ true,
              /* srcs */ files,
              /* outputFile */ outputFile,
              /* shouldAddProjectRootToIncludePaths */ false,
              /* includePaths */ ImmutableSortedSet.<Path>of(),
              /* commandLineArgs */ ImmutableList.<String>of()));

    }
  }

  @Override
  protected String getOutputFileNameFormat() {
    return "%s";
  }
}

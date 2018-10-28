/*
 * Copyright 2015-2018 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.idea.sdk.versionManager.perlbrew;

import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.perl5.PerlBundle;
import com.perl5.PerlIcons;
import com.perl5.lang.perl.idea.execution.PerlCommandLine;
import com.perl5.lang.perl.idea.sdk.host.PerlHostData;
import com.perl5.lang.perl.idea.sdk.versionManager.PerlVersionManagerAdapter;
import com.perl5.lang.perl.idea.sdk.versionManager.PerlVersionManagerData;
import com.perl5.lang.perl.util.PerlRunUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Api to the perlbrew cli
 */
class PerlBrewAdapter extends PerlVersionManagerAdapter {
  static final String PERLBREW_EXEC = "exec";
  static final String PERLBREW_INSTALL = "install";
  static final String PERLBREW_LIST = "list";
  static final String PERLBREW_AVAILABLE = "available";
  static final String PERLBREW_WITH = "--with";
  static final String PERLBREW_QUIET = "-q";
  static final String PERLBREW_VERBOSE = "--verbose";

  public PerlBrewAdapter(@NotNull String versionManagerPath, @NotNull PerlHostData hostData) {
    super(versionManagerPath, hostData);
  }

  /**
   * @see PerlBrewData#patchCommandLine(com.perl5.lang.perl.idea.execution.PerlCommandLine)
   */
  @Nullable
  protected List<String> execWith(@NotNull String distributionId, @NotNull String... commands) {
    List<String> commandsList = ContainerUtil.newArrayList(PERLBREW_EXEC, PERLBREW_QUIET, PERLBREW_WITH, distributionId);
    commandsList.addAll(Arrays.asList(commands));
    return getOutput(commandsList);
  }

  /**
   * Creates a library with {@code libararyName} for the perl with {@code perlVersion}. Optionally invokes the {@code successCallback} on success
   */
  @Nullable
  ProcessOutput createLibrary(@NotNull String perlVersion, @NotNull String libraryName) {
    return getProcessOutput(
      new PerlCommandLine(getVersionManagerPath(), "lib", "create", perlVersion + "@" + libraryName).withHostData(getHostData()));
  }

  /**
   * @return list of {@code perlbrew list} items trimmed or null if error happened
   */
  @Nullable
  protected List<String> getDistributionsList() {
    List<String> output = getOutput(PERLBREW_LIST);
    if (output == null) {
      return null;
    }
    return ContainerUtil.map(output, it -> it.replaceAll("\\(.+?\\)", "").trim());
  }

  public void installPerl(@NotNull Project project,
                          @NotNull String distributionId,
                          @NotNull List<String> params,
                          @Nullable ProcessListener processListener) {
    PerlRunUtil.runInConsole(
      new PerlCommandLine(getVersionManagerPath(), PERLBREW_INSTALL, PERLBREW_VERBOSE, distributionId)
        .withParameters(params)
        .withProject(project)
        .withConsoleTitle(PerlBundle.message("perl.vm.perlbrew.installing.perl", distributionId))
        .withConsoleIcon(PerlIcons.PERLBREW_ICON)
        .withVersionManagerData(PerlVersionManagerData.getDefault())
        .withProcessListener(processListener)
    );
  }

  @Nullable
  @Override
  protected List<String> getAvailableDistributionsList() {
    List<String> rawAvailable = getOutput(PERLBREW_AVAILABLE);
    if (rawAvailable == null) {
      return null;
    }
    return rawAvailable.stream().map(String::trim).filter(StringUtil::isNotEmpty).collect(Collectors.toList());
  }

  @NotNull
  @Override
  protected String getErrorNotificationTitle() {
    return PerlBundle.message("perl.vm.perlbrew.notification.title");
  }
}

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

package com.perl5.lang.perl.idea.sdk.versionManager;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.perl5.PerlBundle;
import com.perl5.lang.perl.idea.execution.PerlCommandLine;
import com.perl5.lang.perl.idea.sdk.host.PerlHostData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Base class for CLI adapter of version manager
 */
public abstract class PerlVersionManagerAdapter {
  private static final Logger LOG = Logger.getInstance(PerlVersionManagerAdapter.class);

  @NotNull
  private final String myVersionManagerPath;
  @NotNull
  private final PerlHostData myHostData;

  public PerlVersionManagerAdapter(@NotNull String versionManagerPath, @NotNull PerlHostData hostData) {
    myVersionManagerPath = versionManagerPath;
    myHostData = hostData;
  }

  @NotNull
  public String getVersionManagerPath() {
    return myVersionManagerPath;
  }

  @NotNull
  public PerlHostData getHostData() {
    return myHostData;
  }

  /**
   * Executes commands in the context of {@code distributionId}
   */
  @Nullable
  protected abstract List<String> execWith(@NotNull String distributionId, @NotNull String... commands);

  /**
   * @return list of installed distributions
   */
  @Nullable
  protected abstract List<String> getDistributionsList();

  /**
   * @return list of available distributions
   */
  @Nullable
  protected abstract List<String> getAvailableDistributionsList();

  @Nullable
  protected List<String> getOutput(@NotNull List<String> parameters) {
    return getOutput(new PerlCommandLine(getVersionManagerPath()).withParameters(parameters));
  }

  /**
   * @return output of perlbew command or null if error happened
   */
  @Nullable
  protected List<String> getOutput(@NotNull PerlCommandLine commandLine) {
    ProcessOutput processOutput = getProcessOutput(commandLine.withHostData(getHostData()));
    if (processOutput == null) {
      return null;
    }
    if (processOutput.getExitCode() == 0) {
      return processOutput.getStdoutLines(true);
    }
    LOG.warn("Process exited with non-zero code. Command line: " + commandLine +
             "; exit code: " + processOutput.getExitCode() +
             "; stdout: " + processOutput.getStdout() +
             "; stderr: " + processOutput.getStderr());
    notifyUser(PerlBundle.message("perl.vm.adapter.notification.message.exitcode"));
    return null;
  }

  @Nullable
  protected ProcessOutput getProcessOutput(@NotNull PerlCommandLine commandLine) {
    try {
      return PerlHostData.execAndGetOutput(commandLine.withHostData(getHostData()));
    }
    catch (ExecutionException e) {
      LOG.warn("Error running " + commandLine, e);
      notifyUser(PerlBundle.message("perl.vm.adapter.notification.message.exception", e.getMessage()));
    }
    return null;
  }

  @Nullable
  protected List<String> getOutput(@NotNull String... parameters) {
    return getOutput(Arrays.asList(parameters));
  }

  @NotNull
  protected String getNotificationGroup() {
    return PerlBundle.message("perl.vm.notification.group");
  }

  @NotNull
  protected abstract String getErrorNotificationTitle();

  private void notifyUser(@NotNull String message) {
    Notifications.Bus.notify(new Notification(
      getNotificationGroup(),
      getErrorNotificationTitle(),
      message,
      NotificationType.ERROR
    ));
  }
}

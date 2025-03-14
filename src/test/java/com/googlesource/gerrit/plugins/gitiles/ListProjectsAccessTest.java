// Copyright (C) 2021 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.gitiles;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.testsuite.project.TestProjectUpdate.allow;
import static com.google.gerrit.extensions.client.ProjectState.HIDDEN;
import static com.google.gerrit.server.group.SystemGroupBackend.ANONYMOUS_USERS;
import static com.google.gerrit.server.group.SystemGroupBackend.REGISTERED_USERS;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.acceptance.testsuite.request.RequestScopeOperations;
import com.google.gerrit.entities.Permission;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.api.projects.BranchInput;
import com.google.gerrit.extensions.api.projects.ConfigInput;
import com.google.gerrit.testing.ConfigSuite;
import com.google.gerrit.util.http.testutil.FakeHttpServletRequest;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.eclipse.jgit.lib.Config;
import org.junit.After;
import org.junit.Test;

@TestPlugin(name = "gitiles", sysModule = "com.googlesource.gerrit.plugins.gitiles.PluginModule")
public class ListProjectsAccessTest extends LightweightPluginDaemonTest {
  @ConfigSuite.Default
  public static Config defaultConfig() {
    // Required config parameters to run Gitiles.
    Config cfg = new Config();
    cfg.setString("gerrit", null, "gitHttpUrl", "http://example.com/gitiles");
    return cfg;
  }

  @Inject private RequestScopeOperations requestScopeOperations;
  @Inject private ProjectOperations projectOperations;

  @After
  public void cleanUp() {
    projectOperations.project(project).forUpdate().removeAllAccessSections().update();
  }

  @Test
  public void listRepositories_repositoryVisibilityIsRespected() throws Exception {
    projectOperations.allProjectsForUpdate().removeAllAccessSections().update();
    projectOperations.newProject().name("visible").create();
    projectOperations.newProject().name("invisible").create();
    projectOperations
        .project(Project.nameKey("visible"))
        .forUpdate()
        .add(allow(Permission.READ).ref("refs/heads/main").group(ANONYMOUS_USERS))
        .update();
    requestScopeOperations.setApiUserAnonymous();

    assertThat(access().listRepositories(null, ImmutableSet.of()).keySet())
        .containsExactly("visible");
  }

  @Test
  public void listBranches_branchVisiblityIsRespected() throws Exception {
    projectOperations.allProjectsForUpdate().removeAllAccessSections().update();
    projectOperations
        .project(project)
        .forUpdate()
        .add(allow(Permission.READ).ref("refs/heads/*").group(REGISTERED_USERS))
        .add(allow(Permission.CREATE).ref("refs/heads/*").group(REGISTERED_USERS))
        .add(allow(Permission.READ).ref("refs/heads/visible").group(ANONYMOUS_USERS))
        .update();
    requestScopeOperations.setApiUser(user.id());
    gApi.projects().name(project.get()).branch("refs/heads/visible").create(new BranchInput());
    gApi.projects().name(project.get()).branch("refs/heads/invisible").create(new BranchInput());
    requestScopeOperations.setApiUserAnonymous();

    assertThat(
            access()
                .listRepositories(
                    null, ImmutableSet.of("refs/heads/visible", "refs/heads/invisible"))
                .values()
                .stream()
                .map(r -> r.branches.keySet())
                .collect(Collectors.toList()))
        .containsExactly(ImmutableSet.of("refs/heads/visible"));
  }

  @Test
  public void listRepositories_hiddenShouldNotBeVisible() throws Exception {
    assertThat(access().listRepositories(null, ImmutableSet.of()).keySet()).contains(project.get());
    ConfigInput ci = new ConfigInput();
    ci.state = HIDDEN;
    gApi.projects().name(project.get()).config(ci);
    assertThat(access().listRepositories(null, ImmutableSet.of()).keySet())
        .doesNotContain(project.get());
  }

  private GerritGitilesAccess access() {
    return plugin
        .getSysInjector()
        .getInstance(GerritGitilesAccess.Factory.class)
        .forRequest(new FakeHttpServletRequest());
  }
}

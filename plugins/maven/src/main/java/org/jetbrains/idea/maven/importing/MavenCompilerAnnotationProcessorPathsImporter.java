// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.idea.maven.importing;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.jdom.Element;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.project.*;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MavenCompilerAnnotationProcessorPathsImporter extends MavenImporter {

  private Logger LOG = Logger.getInstance("#org.jetbrains.idea.maven.importing.MavenCompilerAnnotationProcessorPathsImporter");

  public MavenCompilerAnnotationProcessorPathsImporter() {
    super("org.apache.maven.plugins", "maven-compiler-plugin");
  }

  @Override
  public boolean isApplicable(MavenProject mavenProject) {
    return getConfig(mavenProject, "annotationProcessorPaths") != null;
  }

  @Override
  public void preProcess(Module module,
                         MavenProject mavenProject,
                         MavenProjectChanges changes,
                         IdeModifiableModelsProvider modifiableModelsProvider) {

  }

  @Override
  public void process(IdeModifiableModelsProvider modifiableModelsProvider,
                      Module module,
                      MavenRootModelAdapter rootModel,
                      MavenProjectsTree mavenModel,
                      MavenProject mavenProject,
                      MavenProjectChanges changes,
                      Map<MavenProject, String> mavenProjectToModuleName,
                      List<MavenProjectsProcessorTask> postTasks) {
    String outputDirPath = mavenProject.getAnnotationProcessorDirectory(false);
    rootModel.addGeneratedJavaSourceFolder(outputDirPath, JavaSourceRootType.SOURCE); //todo "ifNotEmpty" flag should be false like in regular source dir
  }

  @Override
  public void resolve(Project project,
                      MavenProject mavenProject,
                      NativeMavenProjectHolder nativeMavenProject,
                      MavenEmbedderWrapper embedder,
                      ResolveContext context) throws MavenProcessCanceledException {
    Element config = getConfig(mavenProject, "annotationProcessorPaths");
    LOG.assertTrue(config != null);

    List<MavenArtifactInfo> artifacts = new ArrayList<>();
    Consumer<Element> addToArtifacts = path -> {
      String groupId = path.getChildTextTrim("groupId");
      String artifactId = path.getChildTextTrim("artifactId");
      String version = path.getChildTextTrim("version");

      String classifier = path.getChildTextTrim("classifier");
      //String type = path.getChildTextTrim("type");

      artifacts.add(new MavenArtifactInfo(groupId, artifactId, version, "jar", classifier));
    };

    for (Element path : config.getChildren("path")) {
      addToArtifacts.consume(path);
    }

    for (Element annotationProcessorPath : config.getChildren("annotationProcessorPath")) {
      addToArtifacts.consume(annotationProcessorPath);
    }

    if (artifacts.isEmpty()) {
      return;
    }

    List<MavenArtifact> annotationProcessorPaths = embedder.resolveTransitively(artifacts, mavenProject.getRemoteRepositories());
    mavenProject.addAnnotationProcessors(annotationProcessorPaths);
  }
}

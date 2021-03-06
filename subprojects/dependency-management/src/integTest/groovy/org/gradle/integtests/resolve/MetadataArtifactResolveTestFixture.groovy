/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ComponentArtifactsResult
import org.gradle.api.artifacts.result.ComponentResult
import org.gradle.api.artifacts.result.UnresolvedComponentResult
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.test.fixtures.file.TestFile

class MetadataArtifactResolveTestFixture {
    private final TestFile buildFile
    final String config
    final ModuleComponentIdentifier id = DefaultModuleComponentIdentifier.newId('some.group', 'some-artifact', '1.0')
    private String requestedComponent
    private String requestedArtifact
    private Class<? extends ComponentResult> expectedComponentResult
    private Throwable expectedException
    private Set<File> expectedMetadataFiles

    MetadataArtifactResolveTestFixture(TestFile buildFile, String config = "compile") {
        this.buildFile = buildFile
        this.config = config
    }

    void basicSetup() {
        buildFile << """
import org.gradle.ivy.IvyModule
import org.gradle.ivy.IvyDescriptorArtifact
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact

configurations {
    $config
}

dependencies {
    $config '$id.displayName'
}
"""
    }

    void configureChangingModule() {
        buildFile << """
dependencies {
    components {
        all { ComponentMetadataDetails details ->
            details.changing = true
        }
    }
}

if (project.hasProperty('nocache')) {
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }
}
"""
    }

    MetadataArtifactResolveTestFixture requestComponent(String component) {
        this.requestedComponent = component
        this
    }

    MetadataArtifactResolveTestFixture requestArtifact(String artifact) {
        this.requestedArtifact = artifact
        this
    }

    MetadataArtifactResolveTestFixture expectResolvedComponentResult() {
        this.expectedComponentResult = ComponentArtifactsResult
        this
    }

    MetadataArtifactResolveTestFixture expectUnresolvedComponentResult(Throwable expectedException) {
        this.expectedComponentResult = UnresolvedComponentResult
        this.expectedException = expectedException
        this
    }

    MetadataArtifactResolveTestFixture expectMetadataFiles(Set<File> metadataFiles) {
        this.expectedMetadataFiles = metadataFiles
        this
    }

    void createVerifyTaskModuleComponentIdentifier() {
        buildFile << """
task verify {
    doLast {
        def deps = configurations.${config}.incoming.resolutionResult.allDependencies as List
        assert deps.size() == 1
        def componentId = deps[0].selected.id

        def result = dependencies.createArtifactResolutionQuery()
            .forComponents(deps[0].selected.id)
            .withArtifacts($requestedComponent, $requestedArtifact)
            .execute()

        assert result.components.size() == 1

        ${createComponentResultVerificationCode()}
"""

        if(expectedComponentResult == UnresolvedComponentResult) {
            buildFile << createUnresolvedComponentResultVerificationCode()
        }

        buildFile << """
        def expectedMetadataFileNames = ${expectedMetadataFiles.collect { "'" + it.name + "'" }} as Set

        for(component in result.resolvedComponents) {
            def resolvedArtifacts = component.getArtifacts($requestedArtifact).findAll { it instanceof ResolvedArtifactResult }
            assert expectedMetadataFileNames.size() == resolvedArtifacts.size()

            if(expectedMetadataFileNames.size() > 0) {
                def resolvedArtifactFileNames = resolvedArtifacts*.file.name as Set
                assert resolvedArtifactFileNames == expectedMetadataFileNames
            }
        }
    }
}
"""
    }

    private String createComponentResultVerificationCode() {
        """
        // Check generic component result
        def componentResult = result.components.iterator().next()
        assert componentResult.id.displayName == '$id.displayName'
        assert componentResult instanceof $expectedComponentResult.name
"""
    }

    private String createUnresolvedComponentResultVerificationCode() {
        """
        // Check unresolved component result
        UnresolvedComponentResult unresolvedComponentResult = (UnresolvedComponentResult)componentResult
        assert unresolvedComponentResult.failure instanceof ${expectedException.getClass().name}
        assert unresolvedComponentResult.failure.message == "$expectedException.message"
"""
}

    void createVerifyTaskForProjectComponentIdentifier() {
        buildFile << """
task verify {
    doLast {
        def rootId = configurations.${config}.incoming.resolutionResult.root.id
        assert rootId instanceof ProjectComponentIdentifier

        dependencies.createArtifactResolutionQuery()
            .forComponents(rootId)
            .withArtifacts($requestedComponent, $requestedArtifact)
            .execute()
    }
}
"""
    }
}

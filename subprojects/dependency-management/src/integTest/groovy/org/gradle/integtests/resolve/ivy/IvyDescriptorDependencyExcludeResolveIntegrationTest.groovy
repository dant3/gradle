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

package org.gradle.integtests.resolve.ivy

import org.gradle.test.fixtures.ivy.IvyModule
import spock.lang.Issue
import spock.lang.Unroll

/**
 * Demonstrates the use of Ivy dependency excludes.
 *
 * @see <a href="http://ant.apache.org/ivy/history/latest-milestone/ivyfile/artifact-exclude.html">Ivy reference documentation</a>
 */
class IvyDescriptorDependencyExcludeResolveIntegrationTest extends AbstractIvyDescriptorExcludeResolveIntegrationTest {
    /**
     * Dependency exclude for a single artifact by using a combination of exclude rules that only match partially or not at all.
     *
     * Dependency graph:
     * a -> b, c
     */
    @Unroll
    def "dependency exclude having single artifact with partially matching #name"() {
        given:
        ivyRepo.module('b').publish()
        ivyRepo.module('c').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')
        addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar'])

        where:
        name                              | excludeAttributes
        'module'                          | [module: 'other']
        'org and module'                  | [org: 'org.gradle.some', module: 'b']
        'name'                            | [name: 'other']
        'name and type'                   | [name: 'b', type: 'sources']
        'name and ext'                    | [name: 'b', ext: 'war']
        'name, type and ext'              | [name: 'b', type: 'javadoc', ext: 'jar']
        'org and name'                    | [org: 'org.gradle.test', name: 'other']
        'org, name and type'              | [org: 'org.gradle.test', name: 'b', type: 'sources']
        'org, name, type and ext'         | [org: 'org.gradle.test', name: 'b', type: 'javadoc', ext: 'jar']
        'org, module and name'            | [org: 'org.gradle.test', module: 'b', name: 'other']
        'org, module, name and type'      | [org: 'org.gradle.test', module: 'b', name: 'b', type: 'sources']
        'org, module, name, type and ext' | [org: 'org.gradle.test', module: 'b', name: 'b', type: 'jar', ext: 'war']
    }

    /**
     * Dependency exclude for a single artifact by using a combination of exclude rules.
     *
     * Dependency graph:
     * a -> b, c
     */
    @Unroll
    def "dependency exclude having single artifact with matching #name"() {
        given:
        ivyRepo.module('b').publish()
        ivyRepo.module('c').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')
        addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(['a-1.0.jar', 'c-1.0.jar'])

        where:
        name                              | excludeAttributes
        'all modules'                     | [module: '*']
        'module'                          | [module: 'b']
        'org and all modules'             | [org: 'org.gradle.test', module: '*']
        'org and module'                  | [org: 'org.gradle.test', module: 'b']
        'all names'                       | [name: '*']
        'wildcard name'                   | [name: 'b*']
        'name'                            | [name: 'b']
        'name and type'                   | [name: 'b', type: 'jar']
        'name and ext'                    | [name: 'b', ext: 'jar']
        'name, type and ext'              | [name: 'b', type: 'jar', ext: 'jar']
        'org and name'                    | [org: 'org.gradle.test', name: 'b']
        'org, name and type'              | [org: 'org.gradle.test', name: 'b', type: 'jar']
        'org, name, type and ext'         | [org: 'org.gradle.test', name: 'b', type: 'jar', ext: 'jar']
        'org, module and name'            | [org: 'org.gradle.test', module: 'b', name: 'b']
        'org, module, name and type'      | [org: 'org.gradle.test', module: 'b', name: 'b', type: 'jar']
        'org, module, name, type and ext' | [org: 'org.gradle.test', module: 'b', name: 'b', type: 'jar', ext: 'jar']
    }

    /**
     * Exclude of transitive dependency with a single artifact by using a combination of exclude rules.
     *
     * Dependency graph:
     * a -> b, c
     * b -> d
     * c -> e
     */
    @Unroll
    def "transitive dependency exclude having single artifact with matching #name"() {
        given:
        ivyRepo.module('d').publish()
        ivyRepo.module('b').dependsOn('d').publish()
        ivyRepo.module('e').publish()
        ivyRepo.module('c').dependsOn('e').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')
        addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(resolvedJars)

        where:
        name                              | excludeAttributes                                                         | resolvedJars
        'all modules'                     | [module: '*']                                                             | ['a-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'module'                          | [module: 'd']                                                             | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org and all modules'             | [org: 'org.gradle.test', module: '*']                                     | ['a-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org and module'                  | [org: 'org.gradle.test', module: 'd']                                     | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'all names'                       | [name: '*']                                                               | ['a-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'wildcard name'                   | [name: 'd*']                                                              | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'name'                            | [name: 'd']                                                               | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'name and type'                   | [name: 'd', type: 'jar']                                                  | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'name and ext'                    | [name: 'd', ext: 'jar']                                                   | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'name, type and ext'              | [name: 'd', type: 'jar', ext: 'jar']                                      | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org and name'                    | [org: 'org.gradle.test', name: 'd']                                       | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org, name and type'              | [org: 'org.gradle.test', name: 'd', type: 'jar']                          | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org, name, type and ext'         | [org: 'org.gradle.test', name: 'd', type: 'jar', ext: 'jar']              | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org, module and name'            | [org: 'org.gradle.test', module: 'd', name: 'd']                          | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org, module, name and type'      | [org: 'org.gradle.test', module: 'd', name: 'd', type: 'jar']             | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org, module, name, type and ext' | [org: 'org.gradle.test', module: 'd', name: 'd', type: 'jar', ext: 'jar'] | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
    }

    /**
     * Exclude of transitive dependency with a single artifact does not exclude its transitive module by using a combination of name exclude rules.
     *
     * Dependency graph:
     * a -> b, c
     * b -> d -> f
     * c -> e
     */
    @Unroll
    def "transitive dependency exclude having single artifact with matching #name does not exclude its transitive module"() {
        given:
        ivyRepo.module('f').publish()
        ivyRepo.module('d').dependsOn('f').publish()
        ivyRepo.module('b').dependsOn('d').publish()
        ivyRepo.module('e').publish()
        ivyRepo.module('c').dependsOn('e').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')
        addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(resolvedJars)

        where:
        name                              | excludeAttributes                                                         | resolvedJars
        'all names'                       | [name: '*']                                                               | ['a-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'wildcard name'                   | [name: 'd*']                                                              | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'name'                            | [name: 'd']                                                               | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'name and type'                   | [name: 'd', type: 'jar']                                                  | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'name and ext'                    | [name: 'd', ext: 'jar']                                                   | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'name, type and ext'              | [name: 'd', type: 'jar', ext: 'jar']                                      | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'org and name'                    | [org: 'org.gradle.test', name: 'd']                                       | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'org, name and type'              | [org: 'org.gradle.test', name: 'd', type: 'jar']                          | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'org, name, type and ext'         | [org: 'org.gradle.test', name: 'd', type: 'jar', ext: 'jar']              | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'org, module and name'            | [org: 'org.gradle.test', module: 'd', name: 'd']                          | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'org, module, name and type'      | [org: 'org.gradle.test', module: 'd', name: 'd', type: 'jar']             | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'org, module, name, type and ext' | [org: 'org.gradle.test', module: 'd', name: 'd', type: 'jar', ext: 'jar'] | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
    }

    /**
     * Exclude of transitive dependency with multiple artifacts by using a combination of exclude rules.
     *
     * Dependency graph:
     * a -> b, c
     * b -> d
     * c -> e
     */
    @Unroll
    def "transitive dependency exclude having multiple artifacts with matching #name"() {
        given:
        ivyRepo.module('d')
                .artifact([:])
                .artifact([type: 'sources', classifier: 'sources', ext: 'jar'])
                .artifact([type: 'javadoc', classifier: 'javadoc', ext: 'jar'])
                .publish()
        ivyRepo.module('b').dependsOn('d').publish()
        ivyRepo.module('e').publish()
        ivyRepo.module('c').dependsOn('e').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')
        addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(resolvedJars)

        where:
        name                              | excludeAttributes                                                         | resolvedJars
        'all modules'                     | [module: '*']                                                             | ['a-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'module'                          | [module: 'd']                                                             | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org and all modules'             | [org: 'org.gradle.test', module: '*']                                     | ['a-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org and module'                  | [org: 'org.gradle.test', module: 'd']                                     | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'all names'                       | [name: '*']                                                               | ['a-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'name'                            | [name: 'd']                                                               | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'name and type'                   | [name: 'd', type: 'jar']                                                  | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0-javadoc.jar', 'd-1.0-sources.jar', 'e-1.0.jar']
        'name and ext'                    | [name: 'd', ext: 'jar']                                                   | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'name, type and ext'              | [name: 'd', type: 'jar', ext: 'jar']                                      | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0-javadoc.jar', 'd-1.0-sources.jar', 'e-1.0.jar']
        'org and name'                    | [org: 'org.gradle.test', name: 'd']                                       | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org, name and type'              | [org: 'org.gradle.test', name: 'd', type: 'jar']                          | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0-javadoc.jar', 'd-1.0-sources.jar', 'e-1.0.jar']
        'org, name, type and ext'         | [org: 'org.gradle.test', name: 'd', type: 'jar', ext: 'jar']              | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0-javadoc.jar', 'd-1.0-sources.jar', 'e-1.0.jar']
        'org, module and name'            | [org: 'org.gradle.test', module: 'd', name: 'd']                          | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'org, module, name and type'      | [org: 'org.gradle.test', module: 'd', name: 'd', type: 'jar']             | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0-javadoc.jar', 'd-1.0-sources.jar', 'e-1.0.jar']
        'org, module, name, type and ext' | [org: 'org.gradle.test', module: 'd', name: 'd', type: 'jar', ext: 'jar'] | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0-javadoc.jar', 'd-1.0-sources.jar', 'e-1.0.jar']
    }

    /**
     * Transitive dependency exclude for a module reachable via alternative path using a combination of exclude rules.
     *
     * Dependency graph:
     * a -> b, c
     * b -> d
     * c -> d
     */
    @Unroll
    def "module with matching #name is not excluded if reachable via alternate path"() {
        given:
        ivyRepo.module('d').publish()
        ivyRepo.module('b').dependsOn('d').publish()
        ivyRepo.module('c').dependsOn('d').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')
        addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(resolvedJars)

        where:
        name                  | excludeAttributes                     | resolvedJars
        'all modules'         | [module: '*']                         | ['a-1.0.jar', 'c-1.0.jar', 'd-1.0.jar']
        'module'              | [module: 'd']                         | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar']
        'org and all modules' | [org: 'org.gradle.test', module: '*'] | ['a-1.0.jar', 'c-1.0.jar', 'd-1.0.jar']
        'org and module'      | [org: 'org.gradle.test', module: 'd'] | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar']
        'name'                | [name: 'd']                           | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar']
        'org and name'        | [org: 'org.gradle.test', name: 'd']   | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar']
    }

    /**
     * Transitive dependency exclude for module reachable by multiple paths for all paths by using a combination of exclude rules.
     *
     * Dependency graph:
     * a -> b, c
     * b -> d
     * c -> d
     */
    @Unroll
    def "module reachable by multiple paths excluded for all paths with matching #name"() {
        given:
        ivyRepo.module('d').publish()
        ivyRepo.module('b').dependsOn('d').publish()
        ivyRepo.module('c').dependsOn('d').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')
        addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        addExcludeRuleToModuleDependency(moduleA, 'c', excludeAttributes)
        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(resolvedJars)

        where:
        name                  | excludeAttributes                     | resolvedJars
        'all modules'         | [module: '*']                         | ['a-1.0.jar']
        'module'              | [module: 'd']                         | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar']
        'org and all modules' | [org: 'org.gradle.test', module: '*'] | ['a-1.0.jar']
        'org and module'      | [org: 'org.gradle.test', module: 'd'] | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar']
        'name'                | [name: 'd']                           | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar']
        'org and name'        | [org: 'org.gradle.test', name: 'd']   | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar']
    }

    /**
     * Exclude of transitive dependency for multiple rules.
     *
     * Dependency graph:
     * a -> b, c
     * b -> d -> f
     * c -> e
     */
    @Unroll
    def "transitive dependency exclude for multiple rules with #name"() {
        given:
        ivyRepo.module('f').publish()
        ivyRepo.module('d').dependsOn('f').publish()
        ivyRepo.module('b').dependsOn('d').publish()
        ivyRepo.module('e').publish()
        ivyRepo.module('c').dependsOn('e').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')

        excludeRules.each { excludeAttributes ->
            addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        }

        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(resolvedJars)

        where:
        name               | excludeRules                                      | resolvedJars
        'no match'         | [[name: 'other'], [name: 'some'], [name: 'more']] | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'all matches'      | [[name: 'b'], [name: 'd'], [name: 'f']]           | ['a-1.0.jar', 'c-1.0.jar', 'e-1.0.jar']
        'partial match'    | [[name: 'other'], [name: 'd'], [name: 'more']]    | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'e-1.0.jar', 'f-1.0.jar']
        'duplicated match' | [[name: 'f'], [name: 'some'], [name: 'f']]        | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar', 'e-1.0.jar']
    }

    /**
     * Exclude of transitive dependency without provided group or module attribute does not exclude its transitive module by using a combination of exclude rules.
     *
     * Dependency graph:
     * a -> b, c
     * b -> d -> f
     * c -> e
     */
    @Issue("https://issues.gradle.org/browse/GRADLE-2674")
    @Unroll
    def "transitive dependency exclude without provided group or module attribute but matching #name does not exclude its transitive module"() {
        given:
        ivyRepo.module('f')
               .artifact([:])
               .artifact([type: 'war'])
               .publish()
        ivyRepo.module('d')
               .artifact([:])
               .artifact([type: 'war'])
               .artifact([type: 'ear'])
               .dependsOn('f')
               .publish()
        ivyRepo.module('b').dependsOn('d').publish()
        ivyRepo.module('e').publish()
        ivyRepo.module('c').dependsOn('e').publish()
        IvyModule moduleA = ivyRepo.module('a').dependsOn('b').dependsOn('c')
        addExcludeRuleToModuleDependency(moduleA, 'b', excludeAttributes)
        moduleA.publish()

        when:
        succeedsDependencyResolution()

        then:
        assertResolvedFiles(resolvedJars)

        where:
        name                            | excludeAttributes              | resolvedJars
        "type 'war'"                    | [type: 'war']                  | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar', 'd-1.0.ear', 'e-1.0.jar', 'f-1.0.jar']
        "ext 'war'"                     | [ext: 'war']                   | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar', 'd-1.0.ear', 'e-1.0.jar', 'f-1.0.jar']
        "type 'war' and conf 'default'" | [type: 'war', conf: 'default'] | ['a-1.0.jar', 'b-1.0.jar', 'c-1.0.jar', 'd-1.0.jar', 'd-1.0.ear', 'e-1.0.jar', 'f-1.0.jar']
        "ext 'jar'"                     | [ext: 'jar']                   | ['a-1.0.jar', 'c-1.0.jar', 'd-1.0.war', 'd-1.0.ear', 'e-1.0.jar', 'f-1.0.war']
    }

    private void addExcludeRuleToModuleDependency(IvyModule module, String dependencyName, Map<String, String> excludeAttributes) {
        module.withXml {
            Node moduleDependency = asNode().dependencies[0].dependency.find { it.@name == dependencyName }
            assert moduleDependency, "Failed to find module dependency with name '$dependencyName'"
            moduleDependency.appendNode(EXCLUDE_ATTRIBUTE, excludeAttributes)
        }
    }
}

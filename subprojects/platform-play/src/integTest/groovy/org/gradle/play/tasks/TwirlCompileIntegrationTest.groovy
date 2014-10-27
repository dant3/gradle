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

package org.gradle.play.tasks
import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class TwirlCompileIntegrationTest extends AbstractIntegrationSpec {

    def setup(){
        buildFile << """
        repositories{
            jcenter()
        }

        configurations{
            twirl
        }

        dependencies{
            twirl "com.typesafe.play:twirl-compiler_2.11:1.0.2"
        }

        task twirlCompile(type:TwirlCompile){
            compilerClasspath = configurations.twirl
            outputDirectory = file('build/twirl')
        }
"""
    }

    /**
     * TODO elaborate
     * */
    def "can run TwirlCompile"(){
        given:
        withTwirlTemplate()
        expect:
        succeeds("twirlCompile")
    }

    def withTwirlTemplate() {
        def templateFile = file("index.html.scala")
        templateFile.createFile()
        templateFile << """@(message: String)

@main("Welcome to Play") {

    @play20.welcome(message)

}

"""
        buildFile << "twirlCompile.source '${templateFile.getAbsolutePath()}'"

    }
}
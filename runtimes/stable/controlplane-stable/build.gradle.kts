/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin

plugins {
    id("application")
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
}

dependencies {
    runtimeOnly(stableLibs.tx.edc.controlplane.postgresql.hashicorp.vault) {
        exclude(group = "org.eclipse.edc", "vault-hashicorp")
        exclude(module = "bdrs-client")
        exclude(module = "tx-iatp-sts-dim")
    }
    runtimeOnly(project(":runtimes:stable:extensions"))
    runtimeOnly(stableLibs.edc.identity.trust.sts.remote.client)
    runtimeOnly(stableLibs.edc.auth.oauth2.client)
    runtimeOnly(stableLibs.edc.api.management.dataplaneselector)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")

}

edcBuild {
    publish.set(false)
}

// configure the "dockerize" task
tasks.register("dockerize", DockerBuildImage::class) {
    val dockerContextDir = project.projectDir
    dockerFile.set(file("$dockerContextDir/src/main/docker/Dockerfile"))
    images.add("${project.name}:${stableLibs.versions.tractusx.get()}")
    images.add("${project.name}:latest")
    // specify platform with the -Dplatform flag:
    if (System.getProperty("platform") != null)
        platform.set(System.getProperty("platform"))
    buildArgs.put("JAR", "build/libs/${project.name}.jar")
    inputDir.set(file(dockerContextDir))
    dependsOn(tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME))
}
plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

group = "es.nexus.neurbia"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    runtimeOnly(project(":src:target-node-directory-api"))
    runtimeOnly(project(":src:federated-catalog-asset-filter"))


    implementation(libs.edc.runtime.core)
    implementation(libs.edc.connector.core)
    runtimeOnly(libs.edc.boot)
    runtimeOnly(libs.edc.control.plane.core)
    implementation(libs.edc.configuration.filesystem)
    runtimeOnly(libs.edc.token.core)
    implementation(libs.edc.http)
    runtimeOnly(libs.edc.dsp)
    implementation(libs.edc.iam.mock)

    implementation("org.eclipse.edc:control-plane-sql:0.14.1")
    implementation("org.eclipse.edc:transaction-local:0.14.1")
    implementation("org.eclipse.edc:sql-bootstrapper:0.14.1")
    implementation("org.eclipse.edc:sql-pool-apache-commons:0.14.1")
    implementation("org.postgresql:postgresql:42.7.8")
    implementation("org.eclipse.edc:federatedcatalog-feature-sql-bom:0.14.1")
    implementation("org.eclipse.edc:federated-catalog-cache-sql:0.14.1")
    implementation("org.eclipse.edc:target-node-directory-sql:0.14.1")
    implementation("tools.jackson.core:jackson-databind:3.0.1")
    implementation(libs.jakarta.rsApi)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("standalone-fc.jar")
    dependsOn(distTar, distZip)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

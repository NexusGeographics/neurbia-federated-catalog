plugins {
    `java-library`
    id("application")
}

dependencies {
    implementation(libs.edc.fc.spi.crawler)
    implementation(libs.jakarta.rsApi)
    implementation(libs.edc.boot)
    implementation(libs.edc.http)
    implementation("org.eclipse.edc:json-ld-spi:0.14.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.17.0")
    implementation("org.eclipse.edc:transaction-datasource-spi:0.14.1")
    implementation("org.eclipse.edc:transaction-spi:0.14.1")
    implementation("org.eclipse.edc:sql-lib:0.14.1")
    implementation("javax.validation:validation-api:2.0.1.Final")
}
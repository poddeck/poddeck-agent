plugins {
  id("java")
  id("io.freefair.lombok") version "9.1.0"
}

group = "io.poddeck"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_25
java.targetCompatibility = JavaVersion.VERSION_25

repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.github.com/poddeck/poddeck-common")
    credentials {
      username = System.getenv("GITHUB_USERNAME") ?: findProperty("github.username") as String?
      password = System.getenv("GITHUB_TOKEN") ?: findProperty("github.token") as String?
    }
  }
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:6.0.1"))
  testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")

  implementation("com.google.inject:guice:7.0.0")

  implementation("com.google.guava:guava:33.5.0-jre")

  implementation("org.projectlombok:lombok:1.18.42")
  annotationProcessor("org.projectlombok:lombok:1.18.42")
  testImplementation("org.projectlombok:lombok:1.18.42")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

  implementation("org.json:json:20250517")

  implementation("org.apache.commons:commons-configuration2:2.12.0")
  implementation("commons-beanutils:commons-beanutils:1.11.0")

  implementation("io.grpc:grpc-stub:1.76.0")
  implementation("io.grpc:grpc-protobuf:1.76.0")
  implementation("io.grpc:grpc-netty:1.76.0")

  implementation("io.kubernetes:client-java:24.0.0")

  implementation("io.poddeck:common:1.0.0-SNAPSHOT")
}

tasks.test {
  useJUnitPlatform()
}

tasks.named<Jar>("jar") {
  manifest {
    attributes["Main-Class"] = "io.poddeck.agent.AgentApplication"
  }
  from(sourceSets.main.get().output)

  duplicatesStrategy = DuplicatesStrategy.INCLUDE

  from({
    configurations.runtimeClasspath.get()
      .filter { it.name.endsWith("jar") }
      .map { zipTree(it) }
  }) {
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
  }
}
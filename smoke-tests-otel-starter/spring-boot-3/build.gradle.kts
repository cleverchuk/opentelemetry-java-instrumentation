plugins {
  id("otel.java-conventions")
  alias(springBoot31.plugins.versions)
  id("org.graalvm.buildtools.native")
}

description = "smoke-tests-otel-starter-spring-boot-3"

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_17)
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
  runtimeOnly("com.h2database:h2")
  implementation("org.apache.commons:commons-dbcp2")
  implementation("org.springframework.kafka:spring-kafka")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

  implementation(project(":smoke-tests-otel-starter:spring-boot-common"))
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.testcontainers:kafka")
  testImplementation("org.testcontainers:mongodb")
  testImplementation("org.springframework.boot:spring-boot-starter-test")

  val testLatestDeps = gradle.startParameter.projectProperties["testLatestDeps"] == "true"
  if (testLatestDeps) {
    // with spring boot 3.5.0 versions of org.mongodb:mongodb-driver-sync and org.mongodb:mongodb-driver-core
    // are not in sync
    testImplementation("org.mongodb:mongodb-driver-sync:latest.release")
  }
}

springBoot {
  mainClass = "io.opentelemetry.spring.smoketest.OtelSpringStarterSmokeTestApplication"
}

tasks {
  compileAotJava {
    with(options) {
      compilerArgs.add("-Xlint:-deprecation,-unchecked,none")
      // To disable warnings/failure coming from the Java compiler during the Spring AOT processing
      // -deprecation,-unchecked and none are required (none is not enough)
    }
  }
  compileAotTestJava {
    with(options) {
      compilerArgs.add("-Xlint:-deprecation,-unchecked,none")
      // To disable warnings/failure coming from the Java compiler during the Spring AOT processing
      // -deprecation,-unchecked and none are required (none is not enough)
    }
  }
  checkstyleAot {
    isEnabled = false
  }
  checkstyleAotTest {
    isEnabled = false
  }
}

// To be able to execute the tests as GraalVM native executables
configurations.configureEach {
  exclude("org.apache.groovy", "groovy")
  exclude("org.apache.groovy", "groovy-json")
  exclude("org.spockframework", "spock-core")
}

graalvmNative {
  // See https://github.com/graalvm/native-build-tools/issues/572
  metadataRepository {
    enabled.set(false)
  }

  tasks.test {
    useJUnitPlatform()
    setForkEvery(1)
  }

  // see https://github.com/junit-team/junit5/wiki/Upgrading-to-JUnit-5.13
  // should not be needed after updating native build tools to 0.11.0
  val initializeAtBuildTime = listOf(
    "org.junit.jupiter.api.DisplayNameGenerator\$IndicativeSentences",
    "org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor\$ClassInfo",
    "org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor\$LifecycleMethods",
    "org.junit.jupiter.engine.descriptor.ClassTemplateInvocationTestDescriptor",
    "org.junit.jupiter.engine.descriptor.ClassTemplateTestDescriptor",
    "org.junit.jupiter.engine.descriptor.DynamicDescendantFilter\$Mode",
    "org.junit.jupiter.engine.descriptor.ExclusiveResourceCollector\$1",
    "org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor\$MethodInfo",
    "org.junit.jupiter.engine.discovery.ClassSelectorResolver\$DummyClassTemplateInvocationContext",
    "org.junit.platform.engine.support.store.NamespacedHierarchicalStore\$EvaluatedValue",
    "org.junit.platform.launcher.core.DiscoveryIssueNotifier",
    "org.junit.platform.launcher.core.HierarchicalOutputDirectoryProvider",
    "org.junit.platform.launcher.core.LauncherDiscoveryResult\$EngineResultInfo",
    "org.junit.platform.suite.engine.SuiteTestDescriptor\$LifecycleMethods",
  )
  binaries {
    named("test") {
      buildArgs.add("--initialize-at-build-time=${initializeAtBuildTime.joinToString(",")}")
    }
  }
}

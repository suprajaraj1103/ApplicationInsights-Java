plugins {
  id("ai.java-conventions")
}

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    release.set(11)
  }
}

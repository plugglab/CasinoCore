plugins {
    id("java")
}

group = "me.casinocore"
version = "1.0"

data class BuildVariant(
    val id: String,
    val protectionEnabled: Boolean,
    val demoEnabled: Boolean,
    val classifier: String,
    val mainClass: String,
    val playUsage: String = "/play <game> <bet>, /play dice <bet> [low|medium|high], /play coinflip <create|join|cancel|list>, or /play list",
    val jarExcludes: List<String> = emptyList()
)

val protectedVariant = BuildVariant(
    id = "protected",
    protectionEnabled = true,
    demoEnabled = false,
    classifier = "protected",
    mainClass = "com.casinocore.core.CasinoCore"
)

val normalVariant = BuildVariant(
    id = "normal",
    protectionEnabled = false,
    demoEnabled = false,
    classifier = "normal",
    mainClass = "com.casinocore.core.CasinoCore"
)

val clearVariant = BuildVariant(
    id = "clear",
    protectionEnabled = false,
    demoEnabled = false,
    classifier = "clear",
    mainClass = "com.casinocore.core.CasinoCore"
)

val demoVariant = BuildVariant(
    id = "demo",
    protectionEnabled = false,
    demoEnabled = true,
    classifier = "demo",
    mainClass = "com.casinocore.core.CasinoCoreDemo",
    playUsage = "/play slots <bet>, /play coinflip <create|join|cancel|list>, or /play list",
    jarExcludes = listOf(
        "com/casinocore/api/**",
        "com/casinocore/games/blackjack/**",
        "com/casinocore/games/diceroll/**",
        "com/casinocore/games/doubleup/**",
        "com/casinocore/games/highlow/**",
        "com/casinocore/games/horserace/**",
        "com/casinocore/games/roulette/**",
        "com/casinocore/games/treasure/**",
        "com/casinocore/games/commands/PlayCommand.class",
        "com/casinocore/games/impl/DiceGame.class",
        "com/casinocore/games/impl/Lottery*.class",
        "com/casinocore/games/impl/Wheel*.class",
        "com/casinocore/integrations/**",
        "com/casinocore/simulation/**",
        "com/casinocore/utils/ProtectionManager*.class",
        "com/casinocore/core/CasinoCore.class"
    )
)

fun BuildVariant.resourceProperties(): Map<String, String> = mapOf(
    "project.version" to project.version.toString(),
    "build.variant" to id,
    "build.protection.enabled" to protectionEnabled.toString(),
    "build.demo.enabled" to demoEnabled.toString(),
    "build.main.class" to mainClass,
    "build.play.usage" to playUsage
)

fun org.gradle.api.file.CopySpec.applyResourcePlaceholders(props: Map<String, String>) {
    filesMatching(listOf("plugin.yml", "config.yml")) {
        filter { line ->
            props.entries.fold(line) { current, entry ->
                current.replace("\${${entry.key}}", entry.value)
            }
        }
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

/* =========================
   TESTS
========================= */
tasks.test {
    useJUnitPlatform()
}

/* =========================
   MAIN BUILD (DEFAULT)
========================= */
tasks.processResources {
    val props = normalVariant.resourceProperties()
    inputs.properties(props)
    filteringCharset = "UTF-8"
    applyResourcePlaceholders(props)
}

tasks.jar {
    archiveClassifier.set("")
}

/* =========================
   CUSTOM BUILDS
========================= */

fun registerVariantJar(taskName: String, variant: BuildVariant) {
    val processedResourcesTask = tasks.register("process${variant.id.replaceFirstChar { it.uppercase() }}Resources", Copy::class) {
        val props = variant.resourceProperties()
        inputs.properties(props)
        filteringCharset = "UTF-8"
        from("src/main/resources")
        into(layout.buildDirectory.dir("generated/resources/${variant.id}"))
        applyResourcePlaceholders(props)
    }

    tasks.register(taskName, Jar::class) {
        dependsOn(tasks.classes, processedResourcesTask)
        archiveClassifier.set(variant.classifier)
        from(sourceSets.main.get().output.classesDirs)
        from(processedResourcesTask)
        variant.jarExcludes.forEach { pattern -> exclude(pattern) }
    }
}

registerVariantJar("normalJar", normalVariant)
registerVariantJar("protectedJar", protectedVariant)
registerVariantJar("clearJar", clearVariant)
registerVariantJar("demoJar", demoVariant)

/* =========================
   OPTIONAL: BUILD ALL
========================= */
tasks.register("buildAll") {
    dependsOn("build", "normalJar", "protectedJar", "clearJar", "demoJar")
}

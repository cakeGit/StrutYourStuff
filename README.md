# Strut Your Stuff

Strut Your Stuff is a library designed to create struts, blocks that span between two points. Originally an internal system inside Bits 'n' Bobs, it's been extracted into a standalone API that makes the reuse of these systems quick and easy.

## Links

* [Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/strut-your-stuff)
* [Download on Modrinth](https://modrinth.com/mod/strut-your-stuff)
* [Documentation](https://struts.azmod.net)
* [JavaDocs](https://struts-jdoc.azmod.net)

## Features

* **Collision structures:** Struts create physical collision structures, ensuring more polished interaction.
* **Flywheel integration:** Struts can be rendered within Flywheel's rendering system, allowing for improved performance.
* **Surface clipping:** Strut models are clipped to surfaces for a more polished look.

## Getting Started

To start using the library, you'll need to add the Aztech Maven to your `build.gradle` file:

```groovy
repositories {
    maven { // Aztech Maven, home of Strut Your Stuff
        name = "Aztech Maven"
        url = "https://maven.azmod.net/releases"
    }
}
```

Depending on your project's scope, you can include Strut Your Stuff as either an external or internal dependency.

### External Dependency (Preferred)

Versions can be found on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/strut-your-stuff/files/all?page=1&pageSize=20&showAlphaFiles=hide) and [Modrinth](https://modrinth.com/mod/strut-your-stuff/versions).

It is recommended to use Strut Your Stuff as a standard external dependency. This approach ensures you automatically get fixes and improvements, reduces overhead, and helps give credit to the library.

In your `build.gradle` dependencies:

```groovy
dependencies {
    implementation "com.cake.struts:struts:<version>+mc1.21.1"
}
```

You'll also need to declare it as a dependency in your `neoforge.mods.toml`:

```toml
[[dependencies.yourmodid]]
    modId = "struts"
    type = "required"
    versionRange = "[<version>,)"
    ordering = "AFTER"
    side = "BOTH"
```

### Internal Dependency (With Jar-In-Jar)

Versions can be found on [CurseForge](#) and [Modrinth](#).

If you just want a no-fuss, clean single JAR and don't want to make your users download an extra file, using jar-in-jar is totally fine. (Especially for non-create addons, this library is less likely to be used by multiple mods).

Use both `implementation` and `jarJar` to include it directly within your compiled mod:

```groovy
dependencies {
    implementation "com.cake.struts:struts:<version>+mc1.21.1"
    jarJar "com.cake.struts:struts:<version>+mc1.21.1"
}
```

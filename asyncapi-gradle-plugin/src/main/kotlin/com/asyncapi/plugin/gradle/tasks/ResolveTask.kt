package com.asyncapi.plugin.gradle.tasks

import com.asyncapi.v2.model.AsyncAPI
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ConfigurationBuilder
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths

open class ResolveTask: DefaultTask() {

    @get:Input
    var classNames: Array<String> = emptyArray()
    @get:Input
    var packageNames: Array<String> = emptyArray()
    @get:Input
    var schemaFileName: String = "asyncapi"
    @get:Input
    var schemaFileFormat: String = "json"
    @get:Input
    var schemaFilePath: String = "generated/asyncapi"
    @get:Input
    var includeNulls: Boolean = false
    @get:Input
    var prettyPrint: Boolean = true

    @Optional
    @get:Classpath
    var buildClasspath: Iterable<File> = emptySet()
    @get:Classpath
    var classPath: Iterable<File> = emptySet()

    private val objectMapper: ObjectMapper by lazy {
        val instance = when(schemaFileFormat ?: "json") {
            "json" -> {
                ObjectMapper()
            }
            "yaml" -> {
                ObjectMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
            }
            else -> throw GradleException("schemaFileFormat=$schemaFileFormat not recognized")
        }

        if (!includeNulls) {
            instance.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }

        instance
    }

    @TaskAction
    @Throws(GradleException::class)
    fun resolve() {
        val classPathUrls = classPath.map {
            try { it.toURI().toURL() } catch (exception: Exception) { throw GradleException("Can't create classpath for task: $name", exception) }
        }.toSet()
        val buildClasspathUrls = buildClasspath.map {
            try { it.toURI().toURL() } catch (exception: Exception) { throw GradleException("Can't create classpath for task: $name", exception) }
        }.toSet()

        classPathUrls.plus(buildClasspathUrls)
        val classLoader = URLClassLoader(classPathUrls.toTypedArray())

        logger.info("Resolving AsyncAPI specification..")

        if (classNames.isEmpty() && packageNames.isEmpty()) {
            throw GradleException("classNames or packageNames are required")
        }

        classNames.let {
            logger.info("Handling class names")

            it.forEach {className ->
                generateSchema(loadClass(classLoader, className))
            }
        }

        packageNames.let {
            logger.info("Handling package names")

            it.forEach {packageName ->
                val classes = loadClasses(packageName, classLoader)
                classes.forEach(this::generateSchema)
            }
        }

    }

    @Throws(GradleException::class)
    private fun writeSchema(schema: String, schemaName: String) {
        val fileName = when(schemaFileFormat) {
            "json" -> "$schemaName-$schemaFileName.json"
            "yaml" -> "$schemaName-$schemaFileName.yaml"
            else -> throw GradleException("schemaFileFormat=$schemaFileFormat not recognized")
        }

        val dirPath = if (schemaFilePath.isBlank()) {
            Paths.get("asyncapi-schemas")
        } else {
            Paths.get(schemaFilePath)
        }

        logger.info("Generated schema: $schemaName\n$schema")

        File(Files.createDirectories(dirPath).toFile(), fileName).writeText(schema, Charsets.UTF_8)
    }

    @Throws(GradleException::class)
    private fun generateSchema(schemaClass: Class<*>) {
        val schema = try {
            val foundAsyncAPI = schemaClass.newInstance()

            if (prettyPrint) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(foundAsyncAPI)
            } else {
                objectMapper.writeValueAsString(foundAsyncAPI)
            }
        } catch (exception: Exception) {
            throw GradleException("Can't serialize ${schemaClass.simpleName} because ${exception.message}", exception)
        }

        writeSchema(schema, schemaClass.simpleName)
    }

    @Throws(GradleException::class)
    private fun loadClass(classLoader: ClassLoader, className: String): Class<*> {
        return try {
            classLoader.loadClass(className)
        } catch (classNotFoundException: ClassNotFoundException) {
            throw GradleException("Loading class error: $className", classNotFoundException)
        }
    }

    @Throws(GradleException::class)
    private fun loadClasses(packageName: String, classLoader: ClassLoader): Set<Class<*>> {
        return try {
            val reflections = Reflections(ConfigurationBuilder()
                    .forPackages(packageName)
                    .addScanners(SubTypesScanner(false))
                    .addUrls((classLoader as URLClassLoader).urLs.asList())
                    .addClassLoader(classLoader)
            )
            reflections.getSubTypesOf(AsyncAPI::class.java)
        } catch (exception: Exception) {
            throw GradleException("Loading package error: $packageName", exception)
        }
    }

}
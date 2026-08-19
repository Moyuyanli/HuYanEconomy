package top.mrxiaom.overflow.gateway.loader

import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ServiceLoader
import kotlin.io.path.extension

fun main(args: Array<String>) {
    val directory = Paths.get(args.firstOrNull() ?: "adapters")
    if (!Files.isDirectory(directory)) {
        error("Missing adapter directory: $directory")
    }

    val providerClass = Class.forName("top.mrxiaom.overflow.provider.PlatformProvider")
    val parent = providerClass.classLoader
    Files.list(directory).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.extension.equals("jar", ignoreCase = true) }
            .sorted()
            .forEach { jar ->
                println("==> $jar")
                try {
                    URLClassLoader(arrayOf(jar.toUri().toURL()), parent).use { loader ->
                        val service = ServiceLoader.load(providerClass, loader)
                        val providers = service.toList()
                        val ids = providers.map { provider ->
                            val descriptor = providerClass.getMethod("getDescriptor").invoke(provider)
                            descriptor.javaClass.getMethod("getId").invoke(descriptor)
                        }
                        println("providers=$ids")
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
    }
}

package xveon.roadmap

import java.io.File

object FileSys {
    fun listFiles(path: String): List<File> {
        val dir = File(path)
        return dir.listFiles()?.toList() ?: listOf()
    }

    fun readFile(path: String): String {
        val file = File( path)
        return readFile(file)
    }

    fun readFile(file: File): String {
        return file.readText()
    }

    fun writeFile(path: String, contents: String) {
        val file = File( path)
        createFile(file)
        file.writeText(contents)
    }

    fun createFile(path: String): Boolean {
        val file = File(path)
        return createFile(file)
    }

    fun createFile(file: File): Boolean {
        file.parentFile.mkdirs()
        return file.createNewFile()
    }

    fun removeFile(path: String): Boolean {
        val file = File(path)
        return removeFile(file)
    }

    fun removeFile(file: File): Boolean {
        if (!containsFile(file)) return false
        file.delete()
        return true
    }

    fun containsFiles(path: String): Boolean {
        return listFiles(path).isNotEmpty()
    }

    fun containsFile(path: String): Boolean {
        val file = File(path)
        return containsFile(file)
    }

    fun containsFile(file: File): Boolean {
        return file.exists()
    }
}

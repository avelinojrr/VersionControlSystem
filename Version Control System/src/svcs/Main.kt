package svcs

import java.io.File
import kotlin.system.exitProcess

var userName: String = ""
var trackedFilesList = mutableListOf<String>()
val vcs = File("vcs")
val commitFolder = File("vcs/commits")
val configFile = File("vcs/config.txt")
val indexFile = File("vcs/index.txt")
val logFile = File("vcs/log.txt")

fun main(args: Array<String>) {
    if (!vcs.exists()) vcs.mkdir()
    if (!commitFolder.exists()) commitFolder.mkdir()
    if (!configFile.exists()) configFile.createNewFile()
    if (!indexFile.exists()) indexFile.createNewFile()
    if (!logFile.exists()) logFile.createNewFile()
    trackedFilesList.addAll(indexFile.readLines().toMutableList())
    val help = """
                These are SVCS commands:
                config     Get and set a username.
                add        Add a file to the index.
                log        Show commit logs.
                commit     Save changes.
                checkout   Restore a file.""".trimIndent()
    when (args.firstOrNull()) {
        "--help" -> println(help)
        null -> println(help)
        "config" -> config(args)
        "add" -> add(args)
        "log" -> log()
        "commit" -> commit(args)
        "checkout" -> checkOut(args)
        else -> println("'${args.first()}' is not a SVCS command.")
    }
}
fun config(args: Array<String>){
    when {
        args.size == 1 && (configFile.readText().isEmpty()) -> println("Please, tell me who you are.")
        args.size == 1 -> {
            userName = configFile.readText()
            println("The username is $userName.")
        }
        args.size == 2 -> {
            userName = args[1]
            configFile.writeText(userName)
            println("The username is $userName.")
        }
    }
}
fun add(args: Array<String>){
    when {
        args.size == 1 && (indexFile.readText().isEmpty()) -> println("Add a file to the index.")
        args.size == 1 -> {
            println("Tracked files:")
            for (fileName in trackedFilesList) println(fileName)
        }
        args.size == 2 -> {
            if (File("./${args[1]}").exists()) {
                indexFile.appendText(args[1] + "\n")
                println("The file \'${args[1]}\' is tracked.")
                trackedFilesList.add(args[1])
                return
            }
            println("Can't find \'${args[1]}\'.")
        }
    }
}
fun commit(args: Array<String>) {
    val msg = try { args[1] } catch (e:IndexOutOfBoundsException) {
        println("Message was not passed.")
        exitProcess(0) }
    val commitHash = indexFile.readText().hashCode().toString() + msg.hashCode().toString()
    val commitHashDirectory = "vcs/commits/$commitHash"
    if ( indexFile.readText() == "" || filesAreNotChanged()) {
        println("Nothing to commit.")
        exitProcess(0)
    }
    File(commitHashDirectory).mkdir()
    trackedFilesList.forEach {
        val content = File("./$it").readText()
        File("$commitHashDirectory/$it").createNewFile()
        File("$commitHashDirectory/$it").writeText(content)
    }
    logFile.writeText("commit $commitHash\nAuthor: ${configFile.readText()}\n$msg" +
            if (logFile.readText() == "") "" else "\n${logFile.readText()}")
    println("Changes are committed.")
}
fun log() {
    if (logFile.readText() == "") {
        println("No commits yet.")
        exitProcess(0)
    }
    println(logFile.readText())
}
fun filesAreNotChanged(): Boolean {
    if (logFile.readText() == "") return false
    val lastCommit = logFile.readLines()[0].substringAfterLast(" ")
    val listOfFilesFromLastCommit = File("vcs/commits/$lastCommit").listFiles() ?: return false
    val listOfFileNames = listOfFilesFromLastCommit.map { it.name }.sorted()
    if (trackedFilesList.sorted() != listOfFileNames) return false
    trackedFilesList.forEach {
        try {
            if (File("./$it").readText() != File("vcs/commits/$lastCommit/$it")
                    .readText()) return false
        } catch (e: Exception) {
            return false
        }
    }
    return true
}
fun checkOut(args: Array<String>) {
    val commit = try { args[1] } catch (e:IndexOutOfBoundsException) {
        println("Commit id was not passed.")
        exitProcess(0) }
    if(logFile.readText().isEmpty()) {
        println("here2")
        println("Commit does not exist.")
        exitProcess(0)
    }
    val commitHashes = File("vcs/commits").walkTopDown().filter { it.isDirectory }
        .map { it.toString().substringAfterLast("\\")}.toList()
    if (commit !in commitHashes) {
        println("Commit does not exist.")
        if(commit == "1910279870-303977273") println(commitHashes)
        exitProcess(0)
    }
    val wantedCommit = File("vcs/commits/$commit").listFiles() ?: return
    val wantedCommitNames = wantedCommit.map { it.name }
    wantedCommitNames.forEach {
        File("vcs/commits/$commit/$it").copyTo(File(it), overwrite = true)
    }
    println("Switched to commit $commit.")
}
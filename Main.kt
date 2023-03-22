package svcs
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

const val HELP_PRINT= """These are SVCS commands:
config     Get and set a username.
add        Add a file to the index.
log        Show commit logs.
commit     Save changes.
checkout   Restore a file."""

fun main(args: Array<String>){

    val vcs = File("vcs")
    if (!vcs.exists())vcs.mkdir()
    val index = vcs.resolve("index.txt")
    val config = vcs.resolve("config.txt")
    val commits = vcs.resolve("commits")
    val logtxt = vcs.resolve("log.txt")
    if (!commits.exists())commits.mkdir()
    commits.setReadable(true)
    commits.setWritable(true)
    if (!logtxt.exists())logtxt.createNewFile()
    if (!index.exists())index.createNewFile()
    if (!config.exists())config.createNewFile()


    if (args.isNotEmpty()){

        when(args[0]){
            "config"-> {
                configImplementation( isEmpty(config),config.readText(),
                    getAmountArgumentsString(args),config)
            }
            "--help" -> {
                println(HELP_PRINT)
            }
            "add" -> {
                addImplementation( isEmpty(index),index, getAmountArgumentsString(args)
                )
            }
            "log" -> {
                logImplementation( isEmpty(logtxt),logtxt)
            }
            "commit"-> {
                commitImplementation(
                    getAmountArgumentsString(args),logtxt,config,commits,index,vcs)
            }
            "checkout" -> {
                checkoutImplementation(getAmountArgumentsString(args),commits,index,vcs,args )
            }

            else -> {
                println("'${args[0]}' is not a SVCS command.")
            }
        }

    }else println(HELP_PRINT)

}

// /**/   --- Utilities ---/**/


fun sortCommitDirectory(shaString: String, parentFile: File): Boolean{
    var b = false
    parentFile.walkTopDown().forEach { if (it.name == shaString) b = true}
    return b

}

fun isEmpty(file: File): Boolean {
    if (file.exists() && file.isFile && file.readText().isEmpty()
    ){
        return true
    }else{
        if (file.exists() && file.isDirectory && file.list().isNullOrEmpty()){
            return true
        }
        return  false
    }
}


fun sha1(input: String): String = hashString(input,"SHA")
fun hashString(input: String, type: String): String {
    return MessageDigest.getInstance(type).digest(input.toByteArray())
        .fold("", { str, it -> str + "%02x".format(it) })

}


fun getAmountArgumentsString(amountArgs:Array<String>):String{
    return if (amountArgs.size>1){
        amountArgs[1]
    }else{
        ""
    }

}

//---------/**/ Implementations-------//

fun checkoutImplementation(hashString: String,commitFile:File,indexFile:File, rootFile: File,args1: Array<String>){


    var count = 0

    if (!isEmpty(commitFile)){

        if (getAmountArgumentsString(args1).isEmpty()){

            println("Commit id was not passed.")
        }else{
            commitFile.listFiles()?.forEach { if (it.name == hashString){
                count=1
            }

            }
            if (count == 1) {
                println("Switched to commit ${hashString}.")
                val hashDirectory = File("${commitFile}/${hashString}")



                Files.copy( hashDirectory.listFiles()?.elementAt(0)?.toPath(),
                    rootFile.canonicalFile.parentFile?.listFiles()
                        ?.first { it.name == indexFile.readLines().elementAt(0) }
                        ?.toPath(),StandardCopyOption.REPLACE_EXISTING)

                Files.copy( hashDirectory.listFiles()?.elementAt(1)?.toPath(),
                    rootFile.canonicalFile.parentFile?.listFiles()
                        ?.first { it.name == indexFile.readLines().elementAt(1 )}
                        ?.toPath(),StandardCopyOption.REPLACE_EXISTING)


            } else println("Commit does not exist.")

        }


    }



}


fun createCommitDirectory(parentFile: File, directoryIdHashName: String, indexFile: File,rootFile: File){

    val directory = File("${parentFile}/${directoryIdHashName}")
    directory.mkdirs()
    directory.setReadable(true)
    directory.setWritable(true)
    val file1 = directory.resolve(indexFile.readLines().elementAt(0))
    val file2 = directory.resolve(indexFile.readLines().elementAt(1))

    file1.createNewFile()
    file1.setReadable(true)
    file2.createNewFile()
    file2.setReadable(true)

    Files.copy(
        rootFile.canonicalFile.parentFile?.listFiles()
            ?.first { it.name == indexFile.readLines().elementAt(0) }?.toPath(),file1.toPath(),StandardCopyOption.REPLACE_EXISTING)
    Files.copy(
        rootFile.canonicalFile.parentFile?.listFiles()
            ?.first { it.name == indexFile.readLines().elementAt(1) }?.toPath(),file2.toPath(),StandardCopyOption.REPLACE_EXISTING)

}




fun commitImplementation(string: String, logFile:File, configFile: File, parentFile:File, indexFile: File,rootFile: File){
    var hashString = ""
    when(string){
        ""-> {
            println("Message was not passed.")
        }
        else -> {
            if (indexFile.readText().isNotEmpty()){

                hashString =   rootFile.canonicalFile.parentFile?.listFiles()
                    ?.first { it.name == indexFile.readLines().elementAtOrNull(0) }
                    ?.readLines().toString()  + rootFile.canonicalFile.parentFile?.listFiles()
                    ?.first { it.name == indexFile.readLines().elementAtOrNull(1) }
                    ?.readLines().toString()

                if (isEmpty(parentFile)|| !sortCommitDirectory(sha1(hashString),parentFile)){
                    val newText: String = ("""

                        commit ${sha1(hashString)}
                        Author: ${configFile.readText()}
                        ${string}

                    """.trimIndent())

                    val oldText = logFile.readText()
                    val sb  = StringBuilder(newText).append(oldText)

                    logFile.writeText(sb.toString())

                    createCommitDirectory(parentFile,sha1(hashString),indexFile,rootFile)
                    println("Changes are committed.")
                }
                else{
                    println("Nothing to commit.")
                }

            }
        }
    }

}

fun logImplementation(empty: Boolean, file: File){

    if (empty){
        println("No commits yet.")
    }else{
        println(file.readText())
    }

}


fun configImplementation(empty: Boolean, oldUserName: String, string:String, file: File){

    when(string){

        "" -> return if (empty){
            println("Please, tell me who you are.")
        }else{
            println("The username is ${oldUserName}.")
        }
        else->  {
            file.writeText(string)
            println(" The username is ${string}.")
        }

    }

}

fun addImplementation(empty: Boolean, file: File, string: String){

    when(string){
        "" ->  return if (empty){
            println("Add a file to the index.")

        }else{
            println("Tracked files:")
            for (file in file.readLines()){
                println("${file}")

            }
        }
        else ->  if (file.readLines().size<2){
            file.appendText(string)
            file.appendText("\n")
            println("The file '${string}' is tracked.")
        }else{
            println("Can't find '${string}'.")
        }
    }

}


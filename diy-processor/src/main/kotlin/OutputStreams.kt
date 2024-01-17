import java.io.OutputStream

fun OutputStream.append(string: String) {
  write(string.toByteArray())
}

fun OutputStream.appendLine(line: String = "") {
  append("$line\n")
}
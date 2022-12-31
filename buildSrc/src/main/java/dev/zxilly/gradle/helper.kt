package dev.zxilly.gradle

import org.codehaus.groovy.runtime.ProcessGroovyMethods

fun String.exec(): String {
    val str = ProcessGroovyMethods.getText(ProcessGroovyMethods.execute(this))
    return str.trim()
}
package dev.zxilly.gradle

import org.codehaus.groovy.runtime.ProcessGroovyMethods

fun String.exec(): String {
    return ProcessGroovyMethods.getText(ProcessGroovyMethods.execute(this))
}
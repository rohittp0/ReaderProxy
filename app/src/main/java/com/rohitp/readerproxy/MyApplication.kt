package com.rohitp.readerproxy

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree

class HyperlinkedDebugTree(private val showMethodName: Boolean = true) : DebugTree() {
    override fun createStackElementTag(element: StackTraceElement) =
        with(element) { "($fileName:$lineNumber) ${if (showMethodName) " $methodName()" else ""}" }

}

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG)
            Timber.plant(HyperlinkedDebugTree(false))
    }
}

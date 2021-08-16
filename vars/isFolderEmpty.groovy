def call() {
    if (isUnix()) {
        def myDirectory = sh script: "ls", returnStdout: true
        return null == myDirectory || "" == myDirectory
    } else {
        def isEmpty = bat script: """@echo off
setlocal
set _TMP=
for /f "delims=" %%a in ('dir /a /b %1') do set _TMP=%%a

IF {%_TMP%}=={} (
  exit /b 0
) ELSE (
  exit /b 1
)""", returnStatus: true
        return isEmpty
    }
}

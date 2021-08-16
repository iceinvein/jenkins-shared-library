def call(String port) {
    if (isUnix()) {
        sh """pid="\$(sudo netstat -tlnp | awk '/:${port} */ {split(\$NF,a,"/"); print a[1]}')"
            sudo kill \$pid"""
    } else {
        bat """FOR /F "tokens=5 delims= " %%P IN ('netstat -a -n -o ^| findstr 0.0.0.0:${port}') DO TaskKill.exe /f /PID %%P"""
    }
    sleep 15
}

def call(String urlString) {
    echo "Checking online status of ${urlString}.."

    def url = new URL(urlString)
    def connection = (HttpURLConnection) url.openConnection()
    int responseCode

    connection.setRequestMethod('GET')
    try {
        connection.connect()
        responseCode = connection.getResponseCode()
        connection = null
    } catch (UnknownHostException | ConnectException ignored) {
        return false
    }
    return responseCode == 200
}

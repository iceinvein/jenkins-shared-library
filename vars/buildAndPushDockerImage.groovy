import groovy.json.JsonSlurperClassic

def call(String dockerUrl, String imageName, String tag, String registryConfig, String registryAuth, String gitUrl,
         String gitCredential,
         LinkedHashMap<String, String> buildArgs) {
    buildDockerImage(dockerUrl, imageName, tag ?: 'latest', registryConfig, getGitUrl(gitUrl, gitCredential), buildArgs)
    executeApi(getUrl(getDockerPushUrl(dockerUrl, imageName, tag ?: 'latest'), 'X-Registry-Auth', registryAuth))
}

def buildDockerImage(String dockerUrl, String imageName, String tag, String registryConfig, String gitUrl,
                     LinkedHashMap<String, String> buildArgs) {
    LinkedHashMap<String, String> parameters = ['t'     : "${imageName}:${tag}",
                                                'pull'  : '',
                                                'remote': gitUrl,]

    if (!buildArgs.isEmpty()) {
        parameters.put('buildargs', getBuildArgsParameters(buildArgs))
    }

    return executeApi(getUrl(getDockerBuildUrl(dockerUrl, parameters), 'X-Registry-Config', registryConfig))
}

static def getBuildArgsParameters(LinkedHashMap<String, String> buildArgs) {
    return "{${buildArgs.collect { k, v -> "\"$k\":\"$v\"" }.join(',')}}"
}

static def getDockerBuildUrl(String dockerUrl, LinkedHashMap<String, String> parameters) {
    return "${dockerUrl}/build?${getParametersString(parameters)}"
}

static def getDockerPushUrl(String dockerUrl, String imageName, String tag) {
    return "${dockerUrl}/images/${URLEncoder.encode(imageName, 'UTF-8')}/push?tag=${URLEncoder.encode(tag, 'UTF-8')}"
}

static def getUrl(String urlString, String requestPropertyType, String requestPropertyValue) {
    URL url = new URL(urlString)
    HttpURLConnection connection = (HttpURLConnection) url.openConnection()

    connection.setRequestMethod('POST')
    connection.setRequestProperty(requestPropertyType,
                                  requestPropertyValue)

    return connection
}

static def getParametersString(LinkedHashMap<String, String> params) {
    StringBuilder result = new StringBuilder()

    params.each {
        result.append(URLEncoder.encode(it.key, 'UTF-8'))
        if (it.value.length() > 0) {
            result.append('=')
            result.append(URLEncoder.encode(it.value, 'UTF-8'))
        }
        result.append('&')
    }

    String resultString = result.toString()
    return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString
}

static def getGitUrl(String gitUrl, String credential) {
    StringBuilder sb = new StringBuilder(gitUrl)
    int idx = gitUrl.indexOf('//')

    sb.insert(idx + 2, "${credential}@")

    return sb.toString()
}

def executeApi(HttpURLConnection connection) {
    try {
        StringBuilder response = new StringBuilder()

        connection.connect()
        int responseCode = connection.getResponseCode()

        Scanner httpResponseScanner = new Scanner(connection.getInputStream())

        while (httpResponseScanner.hasNextLine()) {
            if (responseCode == 200) {
                def lineObject = new JsonSlurperClassic().parseText(httpResponseScanner.nextLine())
                if (lineObject?.stream) {
                    response.append(lineObject.stream as String)
                    if (lineObject.stream.contains('non-zero code')) {
                        error(response.toString())
                    }
                } else if (lineObject?.errorDetail) {
                    response.append(lineObject.errorDetail.message as String)
                } else if (lineObject?.status) {
                    response.append(lineObject.status as String)
                    if (lineObject?.progress) {
                        response.append(" ${lineObject?.progress}\n")
                    } else {
                        response.append('\n')
                    }
                } else if (lineObject?.aux) {
                    if (lineObject.aux?.ID) {
                        response.append("${lineObject.aux.ID}\n")
                    }
                }
            } else {
                response.append(httpResponseScanner.nextLine())
            }
        }
        httpResponseScanner.close()

        echo(response.toString())
    } catch (err) {
        error("Error: ${err.toString()}")
    }
}

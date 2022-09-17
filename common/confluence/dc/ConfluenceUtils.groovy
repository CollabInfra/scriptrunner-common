package common.confluence.dc

import groovy.util.logging.Log4j
import groovy.json.JsonSlurper
import com.atlassian.sal.api.net.Request
import groovy.json.JsonBuilder

@Log4j
class ConfluenceUtils {

    ConfluenceUtils() {}

    boolean isInstanceReadOnly() {
        def isReadOnly = false
        def response = AppLink.doRequestWithoutBody("rest/api/accessmode", Request.MethodType.GET)
        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString)
            switch (responseAsJson) {
                case "READ_ONLY":
                    isReadOnly = true
                    break
                case "READ_WRITE":
                    isReadOnly = false
                    break
                default:
                    isReadOnly = false
            }
                    
        }
        return isReadOnly
    }

    Map getClusterInfo() {
        def clusterDetails = [:] as Map
        def response = AppLink.doRequestWithoutBody("rest/zdu/cluster", Request.MethodType.GET)
        switch (response.statusCode) {
            case HttpURLConnection.HTTP_FORBIDDEN:
                clusterDetails.error = "Confluence is not setup in a cluster"
                break
            case HttpURLConnection.HTTP_OK:
                def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
                clusterDetails = responseAsJson
                break
            default:
                clusterDetails.error = "Unexpected status code ${response.statusCode}, message is ${response.responseBodyAsString}"
        }
        return clusterDetails
    }

    ServerInfo getServerInfo() {
        ServerInfo info
        def requestBody = [jsonrpc: "2.0", method: "getServerInfo", params: [], id: 1]
        def response = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)
        switch (response.statusCode) {
            case HttpURLConnection.HTTP_OK:
                info = new ServerInfo()
                def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
                def result = responseAsJson['result'] as Map
                info.majorVersion = result['majorVersion'] as Integer
                info.minorVersion = result['minorVersion'] as Integer
                info.patchLevel = result['patchLevel'] as Integer
                info.isDevBuild = result['developmentBuild'] as boolean
                info.buildId = result['buildId'] as Integer
                info.baseUrl = result['baseUrl'] as String
                break
            default:
                log.warn "Unexpected status code ${response.statusCode}, message is ${response.responseBodyAsString}"
        }
        return info
    }

    class ServerInfo {
        Integer majorVersion
        Integer minorVersion
        Integer patchLevel
        boolean isDevBuild
        Integer buildId
        String baseUrl
    }

}
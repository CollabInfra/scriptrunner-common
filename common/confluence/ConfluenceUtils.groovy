package common.confluence

import groovy.util.logging.Log4j
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.Response

@Log4j
class ConfluenceUtils {

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
}
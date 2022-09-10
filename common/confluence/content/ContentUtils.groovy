package common.confluence.content

import groovy.util.logging.Log4j
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.Response
import common.confluence.AppLink
import common.confluence.content.enums.*
import org.springframework.lang.NonNull
import common.confluence.spaces.Space
import common.confluence.spaces.enums.SpaceType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Log4j
class ContentUtils {

    List<Content> getPages(@NonNull String title, String spaceKey, ContentStatus status = ContentStatus.CURRENT) {
        return getContent(ContentType.PAGE, spaceKey, title, status, null)
    }

    List<Content> getBlogPosts(@NonNull String postingDay, String spaceKey, ContentStatus status = ContentStatus.CURRENT) {
        def listOfContent = [] as ArrayList<Content>

        def matcher = postingDay =~ /[0-9]{4}-[0-9]{2}-[0-9]{2}/
        if (matcher.matches()) {
            listOfContent = getContent(ContentType.BLOG_POST, spaceKey, null, status, postingDay)
        }

        return listOfContent 
    }

    protected List<Content> getContent(@NonNull ContentType contentType, String spaceKey, String pageTitle, ContentStatus contentStatus, String blogPostingDay) {
        def listOfContent = [] as ArrayList<Content>

        def queryParams = [type: contentType.value, status: contentStatus.status, expand: "version,space,body.storage,body.view,ancestors"]
        if (contentType == ContentType.PAGE) {
            queryParams.title = java.net.URLEncoder.encode(pageTitle, "UTF-8")
        }
        if (contentType == ContentType.BLOG_POST) {
            queryParams.postingDay = blogPostingDay
        }
        if (spaceKey) {
            queryParams.spaceKey = spaceKey
        }
        def queryKeys = queryParams.collect{ it }.join("&")
        
        def response = AppLink.doRequestWithoutBody("rest/api/content?${queryKeys}", Request.MethodType.GET)
        def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString)
        def resultSize = responseAsJson['size'] as Integer
        if (resultSize > 0) {
            def results = responseAsJson['results'] as List<Map>
            results.each { result ->
                def content = new Content()
                
                content.title = result['title'] as String
                content.id = result['id'] as Integer
                content.type = ContentType.valueOfType(result['type'] as String)
                content.status = ContentStatus.valueOfStatus(result['status'] as String)
                content.bodyContent = result['body']['storage']['value'] as String

                def ancestors = result['ancestors'] as List<Map>
                if (ancestors.size() == 1) {
                    content.parentId = ancestors[0]['id'] as Integer
                }
                
                def spaceInfo = new Space()
                spaceInfo.key = result['space']['key'] as String
                spaceInfo.name = result['space']['name'] as String
                spaceInfo.id = result['space']['id'] as Integer
                spaceInfo.type = SpaceType.valueOfType(result['space']['id'] as String)
                content.space = spaceInfo

                def version = new Content.Version(content)
                version.number = result['version']['number'] as Integer
                version.userName = result['version']['by'] as String
                version.when = LocalDate.parse(result['version']['when'] as String, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                content.version = version

                listOfContent.add(content)
            }
        }
    
        return listOfContent
    }
}
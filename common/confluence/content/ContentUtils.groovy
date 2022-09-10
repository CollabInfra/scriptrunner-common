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
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.util.SimpleErrorCollection
import com.atlassian.jira.util.ErrorCollection.Reason

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

        def bodyParams = [type: contentType.value, status: contentStatus.status, expand: "version,space,body.storage,body.view,ancestors"]
        if (contentType == ContentType.PAGE) {
            bodyParams.title = java.net.URLEncoder.encode(pageTitle, "UTF-8")
        }
        if (contentType == ContentType.BLOG_POST) {
            bodyParams.postingDay = blogPostingDay
        }
        if (spaceKey) {
            bodyParams.spaceKey = spaceKey
        }
        def queryKeys = bodyParams.collect{ it }.join("&")
        
        def response = AppLink.doRequestWithoutBody("rest/api/content?${queryKeys}", Request.MethodType.GET)
        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString)
            def resultSize = responseAsJson['size'] as Integer
            if (resultSize > 0) {
                def results = responseAsJson['results'] as List<Map>
                results.each { result ->
                    listOfContent.add(parseContentResponse(result))
                }
            }
        }
    
        return listOfContent
    }

    protected Content parseContentResponse(Map result) {
        def content = new Content()
        
        content.title = result['title'] as String
        content.id = result['id'] as Integer
        content.type = ContentType.valueOfType(result['type'] as String)
        content.status = ContentStatus.valueOfStatus(result['status'] as String)
        content.bodyContent = result['body']['storage']['value'] as String

        def ancestors = result['ancestors'] as List<Map>
        if (ancestors.size() > 0) {
            content.parentId = ancestors[ancestors.size() - 1]['id'] as Integer
        }
        
        def spaceInfo = new Space()
        spaceInfo.key = result['space']['key'] as String
        spaceInfo.name = result['space']['name'] as String
        spaceInfo.id = result['space']['id'] as Integer
        spaceInfo.type = SpaceType.valueOfType(result['space']['id'] as String)
        content.space = spaceInfo

        def version = new Content.Version(content)
        version.number = result['version']['number'] as Integer
        version.userName = result['version']['by']['username'] as String
        version.when = LocalDate.parse(result['version']['when'] as String, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        content.version = version

        return content
    }

    Content getContentById(@NonNull Integer contentId, ContentStatus... statuses) {
        def queryParams = [expand: "version,space,body.storage,body.view,ancestors"]
        if (statuses) {
            def listOfStatus = [] as ArrayList<String>
            statuses.each { status ->
                listOfStatus.add(status.value)
            }
            queryParams.status = listOfStatus.join(",")
        }
        def queryKeys = queryParams.collect{ it }.join("&")
       
        def response = AppLink.doRequestWithoutBody("rest/api/content/${contentId}?${queryKeys}", Request.MethodType.GET)
        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
            return parseContentResponse(responseAsJson)
        }
        return null
    }

    ContentOutcome updateContent(@NonNull Content contentDetails) {
        def errorCollector = new SimpleErrorCollection()

        def currentContent = getContentById(contentDetails.id)
        // Content might have been updated by another process between the fetch and update
        // Return an error if the current version is higher than the version we have
        if (currentContent.version.number > contentDetails.version.number) {
            errorCollector.addErrorMessage("Content version is out of sync", Reason.CONFLICT)
            return new ContentOutcome(errorCollector, null, contentDetails)
        }

        contentDetails.version.number++

        def bodyParams = [
            type: contentDetails.type.value, 
            title: contentDetails.title,
            version: [
                number: contentDetails.version.number
            ],
            body: [
                storage: [
                    value: contentDetails.bodyContent,
                    representation: "storage"
                ]
            ],
            space: [key: contentDetails.space.key],
            status: contentDetails.status.value
        ] as HashMap

        if (contentDetails.parentId) {
            bodyParams.ancestors = [
                [id: contentDetails.parentId]
            ]
        }
                
        def response = AppLink.doRequestWithBody("rest/api/content/${contentDetails.id}", new JsonBuilder(bodyParams).toString(), Request.MethodType.PUT)
        def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString)

        switch (response.statusCode) {
            case HttpURLConnection.HTTP_BAD_REQUEST:
                // Can be many things
                errorCollector.addErrorMessage(responseAsJson['message'] as String, Reason.VALIDATION_FAILED)
                break
            case HttpURLConnection.HTTP_NOT_FOUND:
                // if cannot find draft with current content, when the status change
                errorCollector.addErrorMessage(responseAsJson['message'] as String, Reason.NOT_FOUND)
                break
            case HttpURLConnection.HTTP_CONFLICT:
                // Probably the version number who changed
                errorCollector.addErrorMessage(responseAsJson['message'] as String, Reason.CONFLICT)
                break
            case HttpURLConnection.HTTP_OK:
                // Don't do anything
                break
            default:
                errorCollector.addErrorMessage("Unexpected status code: ${response.statusCode}, response body is ${response.responseBodyAsString}")
        }

        return new ContentOutcome(errorCollector, null, contentDetails)
    }

    ContentOutcome createContent(@NonNull String spaceKey, @NonNull String contentBody, @NonNull String title, @NonNull ContentType type, Integer parentId) {
        def errorCollector = new SimpleErrorCollection()

        def contentDetails = null as Content
        
        def queryParams = [
            type: type.value, 
            title: title,
            body: [
                storage: [
                    value: contentBody,
                    representation: "storage"
                ]
            ],
            space: [key: spaceKey],
        ] as HashMap

        if (parentId) {
            queryParams.ancestors = [
                [id: parentId]
            ]
        }
                
        def response = AppLink.doRequestWithBody("rest/api/content", new JsonBuilder(queryParams).toString(), Request.MethodType.POST)
        def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map

        switch (response.statusCode) {
            case HttpURLConnection.HTTP_BAD_REQUEST:
                // Can be many things
                errorCollector.addErrorMessage(responseAsJson['message'] as String, Reason.VALIDATION_FAILED)
                break
            case HttpURLConnection.HTTP_CONFLICT:
                errorCollector.addErrorMessage(responseAsJson['message'] as String, Reason.CONFLICT)
                break
            case HttpURLConnection.HTTP_OK:
                contentDetails = parseContentResponse(responseAsJson)
                break
            default:
                errorCollector.addErrorMessage("Unexpected status code: ${response.statusCode}, response body is ${response.responseBodyAsString}")
        }

        return new ContentOutcome(errorCollector, null, contentDetails)
    }
}
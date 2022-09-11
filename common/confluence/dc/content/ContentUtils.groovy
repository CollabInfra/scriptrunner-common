package common.confluence.dc.content

import groovy.util.logging.Log4j
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.Response
import common.confluence.dc.AppLink
import common.confluence.dc.content.enums.*
import org.springframework.lang.NonNull
import common.confluence.dc.spaces.Space
import common.confluence.dc.spaces.enums.SpaceType
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

    List<ContentOutcome> getComments(@NonNull Content contentDetails) {
        getChildren(contentDetails, ContentType.COMMENT)
    }

    List<ContentOutcome> getChildrenPages(@NonNull Content contentDetails) {
        getChildren(contentDetails, ContentType.PAGE)
    }

    List<ContentOutcome> getAttachments(@NonNull Content contentDetails) {
        getChildren(contentDetails, ContentType.ATTACHMENT)
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

    /**
     * If the content's status is "current", the content will be moved to the trash
     * If the content's status is "trashed", the content will be purged
     * If you wish to move the content to the trash and purge, call the methods two times, like this:
     *     
     * <pre>
     * def createOutcome = utils.createContent("ds", "Allo", "Test page", ContentType.PAGE, null)
     * def deleteOutcome = utils.deleteContent(createOutcome.get())
     * if (deleteOutcome.isValid()) {
     *   utils.deleteContent(deleteOutcome.get())
     * }
     * </pre>
     * 
    */
    ContentOutcome deleteContent(@NonNull Content contentDetails) {
        def errorCollector = new SimpleErrorCollection()
        def uri = "rest/api/content/${contentDetails.id}"
        def isPurge = false

        if (contentDetails.status == ContentStatus.IN_TRASH) {
            uri = uri + "?status=trashed"
            isPurge = true
        }

        def response = AppLink.doRequestWithoutBody(uri, Request.MethodType.DELETE)

        switch (response.statusCode) {
            case HttpURLConnection.HTTP_NOT_FOUND:
                // Returned if there is no content with the given id, or if the calling user does not have permission to trash or purge the content.
                def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
                errorCollector.addErrorMessage(responseAsJson['message'] as String, Reason.NOT_FOUND)
                break
            case HttpURLConnection.HTTP_CONFLICT:
                // Returned if there is a stale data object conflict when trying to delete a draft
                def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
                errorCollector.addErrorMessage(responseAsJson['message'] as String, Reason.CONFLICT)
                break
            case HttpURLConnection.HTTP_NO_CONTENT:
                if (isPurge) {
                    contentDetails = null
                } else {
                    contentDetails.status = ContentStatus.IN_TRASH
                }
                break
            default:
                errorCollector.addErrorMessage("Unexpected status code: ${response.statusCode}, response body is ${response.responseBodyAsString}")
        }

        return new ContentOutcome(errorCollector, null, contentDetails)
    }

    Map getRestrictionsForContent(@NonNull Content contentDetails) {
        def listOfRestrictions = [:] as Map
        def errorCollector = new SimpleErrorCollection()

        def response = AppLink.doRequestWithoutBody("rest/api/content/${contentDetails.id}/restriction/byOperation", Request.MethodType.GET)
        switch (response.statusCode) {
            case HttpURLConnection.HTTP_NOT_FOUND:
                errorCollector.addErrorMessage("Content not found for id ${contentDetails.id}", Reason.NOT_FOUND)
                break
            case HttpURLConnection.HTTP_OK:
                def readUsers = [] as List<Map>
                def writeUsers = [] as List<Map>
                def readGroups = [] as List<Map>
                def writeGroups = [] as List<Map>

                def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
                def readRestrictions = responseAsJson['read']['restrictions']

                def users = readRestrictions['user']['results'] as List<Map>
                users.each { user ->
                    readUsers.add(type: user['type'], username: user['username'])
                }

                def groups = readRestrictions['group']['results'] as List<Map>
                groups.each { group ->
                    readGroups.add(type: group['type'], name: group['name'])
                }

                def writeRestrictions = responseAsJson['update']['restrictions']

                users = writeRestrictions['user']['results'] as List<Map>
                users.each { user ->
                    writeUsers.add(type: user['type'], username: user['username'])
                }

                groups = writeRestrictions['group']['results'] as List<Map>
                groups.each { group ->
                    writeGroups.add(type: group['type'], name: group['name'])
                }

                listOfRestrictions.read = [users: readUsers, groups: readGroups]
                listOfRestrictions.write = [users: writeUsers, groups: writeGroups]

                break
            default:
                errorCollector.addErrorMessage("Unexpected status code: ${response.statusCode}, response body is ${response.responseBodyAsString}")
        }

        listOfRestrictions.errorCollection = errorCollector

        return listOfRestrictions
    }

    void addLabelsToContent(@NonNull Content contentDetails, @NonNull LabelPrefix prefix, String... labelsName) {
        def bodyParams = [] as List<Map>
        labelsName.each { labelName ->
            bodyParams.add([name: labelName, type: prefix.value])
        }

        def response = AppLink.doRequestWithBody("rest/api/content/${contentDetails.id}/label", new JsonBuilder(bodyParams[0]).toString(), Request.MethodType.POST)
        switch (response.statusCode) {
            case HttpURLConnection.HTTP_NOT_FOUND:
                //errorCollector.addErrorMessage("Content not found for id ${contentDetails.id}", Reason.NOT_FOUND)
                break
            case HttpURLConnection.HTTP_OK:
                break
            default:
                log.warn response.statusCode
                log.warn response.responseBodyAsString
                break
        }
    }

    boolean deleteLabelOnContent(@NonNull Content contentDetails, @NonNull Label label) {
        def isSuccess = false
        def response = AppLink.doRequestWithoutBody("rest/api/content/${contentDetails.id}/label?name=${label.name}", Request.MethodType.DELETE)
        switch (response.statusCode) {
            case HttpURLConnection.HTTP_NOT_FOUND:
                //errorCollector.addErrorMessage("Content not found for id ${contentDetails.id}", Reason.NOT_FOUND)
                break
            case HttpURLConnection.HTTP_NO_CONTENT:
                isSuccess = true
                break
            default:
                log.warn response.statusCode
                log.warn response.responseBodyAsString
                break
        }
        return isSuccess
    }

    List<Label> getLabelsForContent(@NonNull Content contentDetails) {
        def listOfLabels = [] as List<Label>

        def response = AppLink.doRequestWithoutBody("rest/api/content/${contentDetails.id}/label", Request.MethodType.GET)
        switch (response.statusCode) {
            case HttpURLConnection.HTTP_NOT_FOUND:
                //errorCollector.addErrorMessage("Content not found for id ${contentDetails.id}", Reason.NOT_FOUND)
                break
            case HttpURLConnection.HTTP_OK:
                def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
                def labelsList = responseAsJson['results'] as List<Map>
                labelsList.each { labelFromServer ->
                    def label = new Label()
                    label.id = labelFromServer['id'] as Integer
                    label.name = labelFromServer['name'] as String
                    label.prefix = LabelPrefix.valueOfPrefix(labelFromServer['prefix'] as String)
                    listOfLabels.add(label)
                }
                break
            default:
                break
        }

        return listOfLabels
    }

    protected List<ContentOutcome> getChildren(@NonNull Content contentDetails, @NonNull ContentType childType) {
        def errorCollector = new SimpleErrorCollection()
        def listOfOutcomes = [] as ArrayList<ContentOutcome>

        def response = AppLink.doRequestWithoutBody("rest/api/content/${contentDetails.id}/child/${childType.value}?expand=version,space,body.storage,body.view,ancestors", Request.MethodType.GET)
        
        switch (response.statusCode) {
            case HttpURLConnection.HTTP_NOT_FOUND:
                // Returned if there is no content with the given id
                errorCollector.addErrorMessage("Content not found for id ${contentDetails.id}", Reason.NOT_FOUND)
                listOfOutcomes.add(new ContentOutcome(errorCollector, null, null))
                break
            case HttpURLConnection.HTTP_OK:
                def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString)
                def resultSize = responseAsJson['size'] as Integer
                if (resultSize > 0) {
                    def results = responseAsJson['results'] as List<Map>
                    results.each { result ->
                        listOfOutcomes.add(new ContentOutcome(errorCollector, null, parseContentResponse(result)))
                    }
                }                
                break
            default:
                errorCollector.addErrorMessage("Unexpected status code: ${response.statusCode}, response body is ${response.responseBodyAsString}")
                listOfOutcomes.add(new ContentOutcome(errorCollector, null, null))
        }

        return listOfOutcomes
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

}
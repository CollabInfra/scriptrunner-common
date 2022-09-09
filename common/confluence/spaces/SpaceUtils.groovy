package common.confluence.spaces

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.Response
import org.springframework.lang.NonNull
import com.atlassian.jira.user.ApplicationUser
import groovy.json.JsonBuilder
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.util.SimpleErrorCollection
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.Group
import common.confluence.AppLink
import groovy.util.logging.Log4j
import common.confluence.LongRunningTaskOutcome
import common.confluence.spaces.enums.*

@Log4j
class SpaceUtils {

    public SpaceUtils() {}

    SpaceOutcome createSpace(@NonNull String spaceKey, @NonNull String spaceName, @NonNull List<ApplicationUser> spaceAdmins, String description) {
        def errorCollector = new SimpleErrorCollection()
        def spaceInfo = new SpaceInfo()

        def requestBody = [key: spaceKey, name: spaceName]
        if (description) {
            requestBody.put(description, description)
        }

        def response = AppLink.doRequestWithBody("rest/api/space", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            errorCollector.addErrorMessage(response.getResponseBodyAsString())
        } else {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString)
            spaceInfo.setName((String)responseAsJson['name'])
            spaceInfo.setKey((String)responseAsJson['key'])
            if (description) {
                spaceInfo.setDescription(description)
            }
            def homePageMap = responseAsJson['homepage'] as Map
            def homePage = [id: homePageMap['id'], title: homePageMap['title'], link: homePageMap['_links']['webui']]
            spaceInfo.setHomePage(homePage)

            spaceAdmins.each { admin ->
                def adminsRequestBody = [jsonrpc: "2.0", method: "addPermissionToSpace", params: ["SETSPACEPERMISSIONS", admin.username, spaceKey], id: 1]
                def adminsResponse = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(adminsRequestBody).toString(), Request.MethodType.POST)
            }

        }
        return new SpaceOutcome(errorCollector, null, spaceInfo)
    }

    /**
     * Return a list of spaces. By default, it will return 25 spaces, of type GLOBAL, and of status CURRENT
     * 
     * @param spaceStatus CURRENT or ARCHIVED
     * @param spaceType GLOBAL or PERSONAL 
     * @param startAt The start point of the collection to return (pagination)
     * @param limit The limit of the number of spaces to return (pagination)
     * @param spacesKey Optional. List of space keys to filter the search on
     *
     * @return a list of spaces found by the request
    */
    List<SpaceOutcome> getSpaces(SpaceStatus spaceStatus = SpaceStatus.CURRENT, SpaceType spaceType = SpaceType.GLOBAL, Integer startAt = 0, Integer pagerLimit = 25, String... spacesKey) {
        def errorCollector = new SimpleErrorCollection()
        def queryParams = [:]
        def spaces = [] as ArrayList<SpaceOutcome>

        if (spaceStatus) {
            queryParams.put("status", spaceStatus.value)
        }
        if (spaceType) {
            queryParams.put("type", spaceType.type)
        }
        if (spacesKey) {
            def counter = 1
            spacesKey.each { key ->
                queryParams.put("spaceKey${counter}", key)
                counter++
            }
        }
        queryParams.put("start", startAt)
        queryParams.put("limit", pagerLimit)
        
        def queryKeys = queryParams.collect{ it }.join("&").replaceAll("spaceKey[0-9]+","spaceKey")

        def response = AppLink.doRequestWithoutBody("rest/api/space?${queryKeys}", Request.MethodType.GET)
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            errorCollector.addErrorMessage(response.getResponseBodyAsString())
        } else {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
            def results = responseAsJson['results'] as List<Map>
            results.each { result ->
                def spaceInfo = getSpace(result['key'] as String)
                spaces.add(spaceInfo)
            }
        }
        return spaces
    }

    /**
     * Return a space, by its key
     * 
     * @param spaceKey Key of the space
     *
     * @return info about the space
    */
    SpaceOutcome getSpace(@NonNull String spaceKey) {
        def spaceInfo = new SpaceInfo()
        def errorCollector = new SimpleErrorCollection()
        def queryParams = [:]
        def spaces = [] as ArrayList<SpaceInfo>
        
        def response = AppLink.doRequestWithoutBody("rest/api/space/${spaceKey}?expand=homepage,description.plain", Request.MethodType.GET)
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            errorCollector.addErrorMessage(response.getResponseBodyAsString())
        } else {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
            spaceInfo.setId(responseAsJson['id'] as Integer)
            spaceInfo.setName(responseAsJson['name'] as String)
            spaceInfo.setKey(responseAsJson['key'] as String)
            def spaceType = responseAsJson['type'] as String
            spaceInfo.setType(SpaceType.valueOfType(spaceType))
            spaceInfo.setDescription(responseAsJson['description']['plain']['value'] as String)
            def homePageMap = responseAsJson['homepage'] as Map
            def homePage = [id: homePageMap['id'], title: homePageMap['title'], link: homePageMap['_links']['webui']]
            spaceInfo.setHomePage(homePage)

            def requestBody = [jsonrpc: "2.0", method: "getSpaceStatus", params: [spaceKey], id: 1]
            def statusResponse = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)
            def statusResponseAsJson = new JsonSlurper().parseText(statusResponse.responseBodyAsString) as Map
            def status = statusResponseAsJson['result'] as String
            spaceInfo.setStatus(SpaceStatus.valueOf(status))
        }

        return new SpaceOutcome(errorCollector, null, spaceInfo)
    }

    /**
     * Return a space, by its key
     *
     * @param spaceKey Key of the space
     *
     * @return info about the space
    */
    String deleteSpace(@NonNull String spaceKey) {
        def spaceInfo = new SpaceInfo()
        def errorCollector = new SimpleErrorCollection()
        def queryParams = [:]
        def spaces = [] as ArrayList<SpaceInfo>

        def response = AppLink.doRequestWithoutBody("rest/api/space/${spaceKey}", Request.MethodType.DELETE)
        if (response.statusCode == HttpURLConnection.HTTP_ACCEPTED) {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
            def uriToStatus = responseAsJson['links']['status']
            return uriToStatus
        }
        return null
    }

    SpaceOutcome archiveSpace(@NonNull String spaceKey) {
        def spaceInfo = getSpace(spaceKey).get()
        def errorCollector = new SimpleErrorCollection()

        if (spaceInfo.status == SpaceStatus.ARCHIVED) {
            errorCollector.addErrorMessage("Space ${spaceKey} is already archived")
        } else {
            def requestBody = [jsonrpc: "2.0", method: "setSpaceStatus", params: [spaceKey, SpaceStatus.ARCHIVED.value], id: 1]
            def statusResponse = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)
            def statusResponseAsJson = new JsonSlurper().parseText(statusResponse.responseBodyAsString) as Map
            def isSuccess = statusResponseAsJson['result'] as boolean
            if (isSuccess) {
                spaceInfo.setStatus(SpaceStatus.ARCHIVED)
            }
        }
        
        return new SpaceOutcome(errorCollector, null, spaceInfo)
    }

    SpaceOutcome restoreSpace(@NonNull String spaceKey) {
        def spaceInfo = getSpace(spaceKey).get()
        def errorCollector = new SimpleErrorCollection()

        if (spaceInfo.status != SpaceStatus.ARCHIVED) {
            errorCollector.addErrorMessage("Space ${spaceKey} is already restored")
        } else {
            def requestBody = [jsonrpc: "2.0", method: "setSpaceStatus", params: [spaceKey, SpaceStatus.CURRENT.value], id: 1]
            def statusResponse = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)
            def statusResponseAsJson = new JsonSlurper().parseText(statusResponse.responseBodyAsString) as Map
            def isSuccess = statusResponseAsJson['result'] as boolean
            if (isSuccess) {
                spaceInfo.setStatus(SpaceStatus.CURRENT)
            }            
        }
        
        return new SpaceOutcome(errorCollector, null, spaceInfo)
    }

    void removeAllPermissionsForAnonymous(@NonNull String spaceKey) {
        EnumSet.allOf(SpacePermission.class).each { permission ->
            removeSpacePermissionForAnonymous(spaceKey, permission)
        }
    }

    boolean removeSpacePermissionForAnonymous(@NonNull String spaceKey, @NonNull SpacePermission permission) {
        def isSuccess = false
        def requestBody = [jsonrpc: "2.0", method: "removeAnonymousPermissionFromSpace", params: [permission.value, spaceKey], id: 1]
        def response = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)        
        def responseAsMap = new JsonSlurper().parseText(response.responseBodyAsString) as Map
        isSuccess = responseAsMap["result"] as boolean
        return isSuccess
    }

    /*
     * Extra protection: if you try add to anonymous as a space admin or exporter,
     * it will not be added.
    */
    boolean addSpacePermissionForAnonymous(@NonNull String spaceKey, @NonNull SpacePermission permission) {
        def isSuccess = false
        if (!permission.equals(SpacePermission.SPACE_ADMIN) && !permission.equals(SpacePermission.SPACE_EXPORT)) {
            def requestBody = [jsonrpc: "2.0", method: "addAnonymousPermissionToSpace", params: [permission.value, spaceKey], id: 1]
            def response = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)        
            def responseAsMap = new JsonSlurper().parseText(response.responseBodyAsString) as Map
            isSuccess = responseAsMap["result"] as boolean
            return isSuccess
        }
        return isSuccess
    }

    void addPermissionsToSpace(@NonNull String spaceKey, @NonNull SpacePermission... permissions, @NonNull String userOrGroupName) {
        permissions.each { permission ->
            addPermissionToSpace(spaceKey, permission, userOrGroupName)

        }
    }

    boolean addPermissionToSpace(@NonNull String spaceKey, @NonNull SpacePermission permission, @NonNull String userOrGroupName) {
        def isSuccess = false
        def requestBody = [jsonrpc: "2.0", method: "addPermissionToSpace", params: [permission.value, userOrGroupName, spaceKey], id: 1]
        def response = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)        
        def responseAsMap = new JsonSlurper().parseText(response.responseBodyAsString) as Map
        isSuccess = responseAsMap["result"] as boolean
        return isSuccess
    }

    boolean removePermissionForSpace(@NonNull String spaceKey, @NonNull SpacePermission permission, @NonNull String userOrGroupName) {
        def isSuccess = false
        def requestBody = [jsonrpc: "2.0", method: "removePermissionFromSpace", params: [permission.value, userOrGroupName, spaceKey], id: 1]
        def response = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)        
        def responseAsMap = new JsonSlurper().parseText(response.responseBodyAsString) as Map
        isSuccess = responseAsMap["result"] as boolean
        return isSuccess
    }

    List<Map> getPermissionsFromSpace(@NonNull String spaceKey) {
        def finalPermissionsList = [] as ArrayList<Map>

        def requestBody = [jsonrpc: "2.0", method: "getSpacePermissionSets", params: [spaceKey], id: 1]
        def response = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)        
        def responseAsMap = new JsonSlurper().parseText(response.responseBodyAsString) as Map
        def listOfPerms = responseAsMap['result'] as List<Map>
        
        listOfPerms.each { permissionSet ->
            def users = [] as ArrayList<String>
            def groups = [] as ArrayList<String>

            def permission = SpacePermission.valueOfPermission(permissionSet['type'] as String)
            def listOfResources = permissionSet['spacePermissions'] as List<Map>
            listOfResources.each { resource ->
            def userName = resource['userName'] as String
                if (userName) {
                    users.add(userName)
                }
                def groupName = resource['groupName'] as String
                if (groupName) {
                    groups.add(groupName)
                }
            }
            finalPermissionsList.add(permission: permission, users: users, groups: groups)
        }
        return finalPermissionsList
    }

    /**
     * Check the status of a long-running task, like a deletion of a Confluence space.
     *
     * @return get() will true if the task is completed, false otherwhise
    */
    LongRunningTaskOutcome isLongTaskCompleted(@NonNull String pathToTask) {
        def errorCollector = new SimpleErrorCollection()

        def response = AppLink.doRequestWithoutBody(pathToTask, Request.MethodType.GET)
        if (response.statusCode == HttpURLConnection.HTTP_OK) {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString) as Map
            def percentComplete = responseAsJson['percentageComplete'] as Integer
            def isSuccess = responseAsJson['successful'] as boolean
            def messages = responseAsJson['successful'] as List<Map>
            if (!isSuccess) {
                messages.each { message ->
                    errorCollector.addErrorMessage(message['translation'] as String)
                }
            }
            if (percentComplete && percentComplete == 100) {
                return new LongRunningTaskOutcome(errorCollector, null, isSuccess)
            }
        } else if (response.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            errorCollector.addErrorMessage("Task ${pathToTask} not found")
        }
        return new LongRunningTaskOutcome(errorCollector, null, false)
    }

    /**
     * List of administrators of a given Confluence space
     * Take note that the groups and users must also be visible in Jira (Crowd or otherwise)
     * 
     * @param spaceKey Key of the space
     * @param groupsToExclude Optional. If you wish to exclude users coming from groups, like confluence-administrators
     *
     * @return a list of users, either individuals or members of groups
    */
    List<ApplicationUser> adminsForSpace(@NonNull String spaceKey, Group... groupsToExclude) {
        def admins = [] as ArrayList<ApplicationUser>
        
        def requestBody = [jsonrpc: "2.0", method: "getSpacePermissionSet", params: [spaceKey, "SETSPACEPERMISSIONS"], id: 1]
        def response = AppLink.doRequestWithBody("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)        
        def permsSet = new JsonSlurper().parseText(response.responseBodyAsString) as Map
        def perms = permsSet["result"]["spacePermissions"] as List
        perms.each { permission ->
            def userName = permission['userName'] as String
            if (userName) {
                def user = ComponentAccessor.userManager.getUserByName(userName)
                admins.add(user)
            }
            def groupName = permission['groupName'] as String
            if (groupName) {
                def group = ComponentAccessor.groupManager.getGroup(groupName)
                if (group && (!groupsToExclude || !groupsToExclude.contains(group))) {
                    admins.addAll(ComponentAccessor.groupManager.getUsersInGroup(group))
                }
            }
        }
        return admins
    }

}


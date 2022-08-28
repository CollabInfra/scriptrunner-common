package common.confluence

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.Response
import org.springframework.lang.NonNull
import com.atlassian.jira.user.ApplicationUser
import groovy.json.JsonBuilder
import com.atlassian.jira.util.SimpleWarningCollection
import com.atlassian.jira.util.WarningCollection
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.util.SimpleErrorCollection
import com.atlassian.jira.bc.ServiceOutcome
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.Group

class SpaceUtils {

    public SpaceUtils() {}

    SpaceOutcome createSpace(@NonNull String spaceKey, @NonNull String spaceName, @NonNull List<ApplicationUser> spaceAdmins, String description) {
        def errorCollector = new SimpleErrorCollection()
        def spaceInfo = new SpaceInfo()

        def requestBody = [key: spaceKey, name: spaceName]
        if (description) {
            requestBody.put(description, description)
        }

        def response = AppLink.doRequest("rest/space", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            errorCollector.addErrorMessage(response.getResponseBodyAsString())
        } else {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString)
            spaceInfo.setName((String)responseAsJson['name'])
            spaceInfo.setKey((String)responseAsJson['key'])
            if (description) {
                spaceInfo.setDescription((String)responseAsJson['description'])
            }
            def homePage = responseAsJson['homePage']['items'] as List<Map>
            spaceInfo.setHomePage(homePage[0])

            spaceAdmins.each { admin ->
                def adminsRequestBody = [jsonrpc: "2.0", method: "addPermissionToSpace", params: ["SETSPACEPERMISSIONS", admin.username, spaceKey], id: 1]
                def adminsResponse = AppLink.doRequest("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(adminsRequestBody).toString(), Request.MethodType.POST)
            }

        }
        return new SpaceOutcome(errorCollector, null, spaceInfo)
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
        def response = AppLink.doRequest("rpc/json-rpc/confluenceservice-v2", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)        
        
        def permsSet = new JsonSlurper().parseText(response.responseBodyAsString) as Map
        def perms = permsSet["spacePermissions"] as List
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

    class SpaceInfo {
        String name
        String key
        List<ApplicationUser> admins
        String description
        Map homePage
    }

    class SpaceOutcome implements ServiceOutcome {
        protected ErrorCollection errorCollection
        protected WarningCollection warningCollection
        protected SpaceInfo spaceInfo

        public SpaceOutcome(ErrorCollection errorCollection, WarningCollection warningCollection, SpaceInfo spaceInfo) {
            this.errorCollection = errorCollection
            this.warningCollection = warningCollection
            this.spaceInfo = spaceInfo
        }

        public SpaceInfo getReturnedValue() {
            return this.spaceInfo
        }

        public SpaceInfo get() {
            return this.spaceInfo
        }

        public ErrorCollection getErrorCollection() {
            return this.errorCollection
        }

        public WarningCollection getWarningCollection() {
            return this.warningCollection
        }

        public boolean isValid() {
            return (errorCollection && !errorCollection.hasAnyErrors())? true: false
        }

        public boolean hasWarnings() {
            return (warningCollection && warningCollection.hasAnyWarnings())? true: false
        }
    }

}
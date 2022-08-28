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

class SpaceUtils {

    public SpaceUtils() {}

    SpaceOutcome createSpace(@NonNull String spaceKey, @NonNull String spaceName, @NonNull List<ApplicationUser> spaceAdmins, String description) {
        def errorCollector = new SimpleErrorCollection()
        def spaceInfo = new SpaceInfo()

        def requestBody = [key: spaceKey, name: spaceName]
        if (description) {
            requestBody.put(description, description)
        }

        def response = AppLink.doRequest("/rest/space", new JsonBuilder(requestBody).toString(), Request.MethodType.POST)
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            errorCollector.addErrorMessage(response.getResponseBodyAsString())
        } else {
            def responseAsJson = new JsonSlurper().parseText(response.responseBodyAsString)
            spaceInfo.setName((String)responseAsJson['name'])
            spaceInfo.setKey((String)responseAsJson['key'])
            spaceInfo.setDescription((String)responseAsJson['description'])
            def homePage = responseAsJson['homePage']['items'] as List<Map>
            spaceInfo.setHomePage(homePage[0])
        }
        return new SpaceOutcome(errorCollector, null, spaceInfo)
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
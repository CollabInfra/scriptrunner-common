package tests.confluence.dc

import com.atlassian.jira.component.ComponentAccessor
import common.confluence.dc.spaces.SpaceUtils
import common.confluence.dc.ConfluenceUtils
import common.confluence.dc.content.ContentUtils
import common.confluence.dc.content.Content
import common.confluence.dc.content.enums.*
import common.confluence.dc.content.Label
import com.atlassian.jira.user.ApplicationUser
import groovy.util.logging.Log4j
import common.confluence.dc.spaces.SpaceOutcome
import spock.lang.*
import common.confluence.dc.spaces.Space

@Log4j
class ConfluenceSpecification extends Specification {

    @Shared
    SpaceUtils spaceUtils = new SpaceUtils()

    @Shared
    ApplicationUser loggedUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

    @Shared
    Space spaceForTest

    def "test space creation"() {
        given: "create a Confluence where we will execute our tests"
        def spaceName = "Space for unit test"
        def spaceKey = "UNITTEST"
        def spaceDescription = "This is a space created by a unit test"

        when: "We invoke this script"
        def outcome = spaceUtils.createSpace(spaceKey, spaceName, [loggedUser], spaceDescription)
        def isValid = outcome.isValid()
        if (isValid) {
            spaceForTest = outcome.get()
        }

        then: "check the result"
        isValid
    }

    def cleanupSpec(){
        if (spaceForTest) {
            spaceUtils.deleteSpace(spaceForTest.key)
        }
    }

}




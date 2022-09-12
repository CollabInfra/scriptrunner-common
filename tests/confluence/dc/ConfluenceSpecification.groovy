package tests.confluence.dc

import com.onresolve.scriptrunner.canned.common.admin.SrSpecification

import com.atlassian.jira.component.ComponentAccessor
import common.confluence.dc.spaces.SpaceUtils
import common.confluence.dc.ConfluenceUtils
import common.confluence.dc.content.ContentUtils
import common.confluence.dc.content.Content
import common.confluence.dc.content.enums.*
import common.confluence.dc.content.Label
import com.atlassian.jira.user.ApplicationUser
import spock.lang.Shared
import groovy.util.logging.Log4j
import common.confluence.dc.spaces.SpaceOutcome

@Log4j
class ConfluenceSpecification extends SrSpecification {

    @Shared
    SpaceUtils spaceUtils = new SpaceUtils()

    @Shared
    ApplicationUser loggedUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

    def "test space creation"() {
        setup: "create a Confluence where we will execute our tests"
        def spaceName = "Space for unit test"
        def spaceKey = "UNITTEST"
        def spaceDescription = "This is a space created by a unit test"

        when: "We invoke this script"
        SpaceOutcome outcome = spaceUtils.createSpace(spaceKey, spaceName, [loggedUser], spaceDescription)

        then: "check the result"
        true
    }

}




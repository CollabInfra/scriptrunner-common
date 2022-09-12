package tests.jira.dc

import common.jira.dc.ProjectUtils
import com.atlassian.jira.bc.project.ProjectCreationData.Builder
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.type.ProjectTypeKeys
import com.onresolve.scriptrunner.canned.common.admin.SrSpecification
import spock.lang.Shared
import com.atlassian.jira.user.ApplicationUser
import common.jira.dc.CreateProjectOutcome

class JiraProjectSpecification extends SrSpecification {
    @Shared
    ProjectUtils projectUtils = new ProjectUtils()

    @Shared
    ApplicationUser loggedUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

    def "create Software project"() {
        setup: "create setup"
        def template = ComponentAccessor.projectManager.getProjectByCurrentKey("TEST")
        def projectData = new Builder().withKey("BIDON").withLead(loggedUser).withType(ProjectTypeKeys.SOFTWARE).withName("Bidon").build()

        when: "create project"
        def createOutcome = projectUtils.createProject(loggedUser, projectData, [loggedUser], template)

        then:
        true

    }
}

/*
def adminUser = ComponentAccessor.userManager.getUserByName("admin")

def projectUtils = new ProjectUtils()
log.warn outcome.isValid()
log.warn outcome.getErrorCollection().hasAnyErrors()
if (outcome.isValid()) {
    def project = outcome.get()
    def isSuccess = projectUtils.archiveProject(adminUser, project)
    if (isSuccess) {
        projectUtils.unarchiveProject(adminUser, project)
    }
    def adminGroup = ComponentAccessor.groupManager.getGroup("jira-administrators")
    def admins = projectUtils.adminsForProject(project,adminGroup)
    log.warn admins
}
*/

import common.jira.ProjectUtils
import com.atlassian.jira.bc.project.ProjectCreationData.Builder
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.type.ProjectTypeKeys

def adminUser = ComponentAccessor.userManager.getUserByName("admin")
def template = ComponentAccessor.projectManager.getProjectByCurrentKey("TEST")
def projectData = new Builder().withKey("BIDON").withLead(adminUser).withType(ProjectTypeKeys.SOFTWARE).withName("Bidon").build()

def projectUtils = new ProjectUtils()
def outcome = projectUtils.createProject(adminUser, projectData, [adminUser], template)
log.warn outcome.isValid()
log.warn outcome.getErrorCollection().hasAnyErrors()
if (outcome.isValid()) {
    def project = outcome.get()
    def isSuccess = projectUtils.archiveProject(adminUser, project)
    if (isSuccess) {
        projectUtils.unarchiveProject(adminUser, project)
    }
}
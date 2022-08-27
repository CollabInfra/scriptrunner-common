package common.jira

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.application.ApplicationAuthorizationService
import com.atlassian.jira.application.ApplicationKeys
import com.atlassian.application.api.ApplicationKey
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.type.ProjectTypeKeys
import com.atlassian.jira.security.roles.ProjectRoleManager

public class UserUtils {

    static userManager = ComponentAccessor.userManager
    static userSearchService = ComponentAccessor.userSearchService
    static userUtil = ComponentAccessor.userUtil
    static ApplicationAuthorizationService appAuthService = ComponentAccessor.getComponent(ApplicationAuthorizationService)
    static ProjectRoleManager projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)

    public boolean haveSoftwareLicense(ApplicationUser user) {
        return haveLicence(user, ApplicationKeys.CORE)
    }

    public boolean haveServiceManagementLicense(ApplicationUser user) {
        return haveLicence(user, ApplicationKeys.SERVICE_DESK)
    }

    public boolean haveCoreLicense(ApplicationUser user) {
        return haveLicence(user, ApplicationKeys.CORE)
    }

    protected boolean haveLicence(ApplicationUser user, ApplicationKey applicationType) {
        if (!user.active) {
            return false
        }
        return appAuthService.canUseApplication(user, applicationType)
    }

    public boolean haveAccessToProject(ApplicationUser user, Project project) {
        def haveAccess = false
        switch (project.projectTypeKey.key) {
            case ProjectTypeKeys.SOFTWARE:
                haveAccess = haveSoftwareLicense(user)
                break
            case ProjectTypeKeys.BUSINESS:
                haveAccess = haveCoreLicense(user)
                break
            case ProjectTypeKeys.SERVICE_DESK:
                haveAccess = haveServiceManagementLicense(user)
                break
        }
        if (haveAccess) {
            def rolesForUser = projectRoleManager.getProjectRoles(user, project)
            if (rolesForUser.isEmpty()) {
                haveAccess = false
            }
        }
        return haveAccess
    }

}
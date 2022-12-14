package common.jira.dc

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.project.ProjectService
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.util.SimpleErrorCollection
import com.atlassian.jira.project.Project
import org.springframework.lang.NonNull
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.bc.project.ProjectCreationData
import com.atlassian.jira.bc.project.ProjectService.CreateProjectValidationResult
import com.atlassian.jira.project.archiving.ArchivedProjectService
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.bc.projectroles.ProjectRoleService
import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.util.SimpleWarningCollection
import com.atlassian.jira.util.WarningCollection
import com.atlassian.crowd.embedded.api.Group

public class ProjectUtils {

    static projectManager = ComponentAccessor.projectManager
    static ProjectService projectService = ComponentAccessor.getComponent(ProjectService)
    static ArchivedProjectService archiveProjectService = ComponentAccessor.getComponent(ArchivedProjectService)
    static ProjectRoleManager projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
    static ProjectRoleService projectRoleService = ComponentAccessor.getComponent(ProjectRoleService)

    static final ProjectRole ADMIN_PROJECT_ROLE = projectRoleManager.getProjectRole("Administrators")

    public ProjectUtils() {}

    /**
     * Create a new Jira project, based or not on another project.
     * If not based on another project, it will use the default schemes
     * 
     * @param globalAdmin An user who can create projects, usually a Jira global admin
     * @param creationData Use <a href="https://docs.atlassian.com/software/jira/docs/api/7.1.6/com/atlassian/jira/bc/project/ProjectCreationData.Builder.html">ProjectCreationData.Builder</a> to generate this data
     * @param projectAdmins List of initial project admins
     * @param source Optional. Use it if you wish to create the project based on the schemes associated to an existing project
     * 
     * @return a tuple containing the new project and an error collection
    */
    CreateProjectOutcome createProject(
            @NonNull ApplicationUser globalAdmin, 
            @NonNull ProjectCreationData creationData, 
            List<ApplicationUser> projectAdmins, 
            Project source) {

        def errorCollector = new SimpleErrorCollection()
        def warningCollector = new SimpleWarningCollection()
        Project createdProject = null
        CreateProjectValidationResult validationResult = null

        def wantedKey = creationData.key
        def wantedName = creationData.name

        if (wantedKey.length() > projectService.maximumKeyLength) {
            errorCollector.addErrorMessage("Project key must be lower than ${projectService.maximumKeyLength}")
        }

        if (wantedName.length() > projectService.maximumNameLength) {
            errorCollector.addErrorMessage("Project name must be lower than ${projectService.maximumNameLength}")
        }

        if (!errorCollector.hasAnyErrors()) {
            if (source) {
                validationResult = projectService.validateCreateProjectBasedOnExistingProject(globalAdmin, source.id, creationData)
            } else {
                validationResult = projectService.validateCreateProject(globalAdmin, creationData)
            }

            if (validationResult && validationResult.isValid()) {
                createdProject = projectService.createProject(validationResult)

                def activeAdmins = projectAdmins.find { it.active == true }
                def nonActiveAdmins = projectAdmins.minus(activeAdmins)
                nonActiveAdmins.each { user ->
                    warningCollector.addWarning("${user.username} was not added because he/she is inactive")
                }

                projectRoleService.addActorsToProjectRole(
                    activeAdmins*.username, 
                    ADMIN_PROJECT_ROLE, 
                    createdProject, 
                    ProjectRoleActor.USER_ROLE_ACTOR_TYPE, 
                    errorCollector)
            } else {
                errorCollector.addErrorCollection(validationResult.errorCollection)
            }
        }

        return new CreateProjectOutcome(errorCollector, warningCollector, createdProject)
    }

    boolean archiveProject(@NonNull ApplicationUser globalAdmin, @NonNull Project project) {
        def validationResult = archiveProjectService.validateArchiveProject(globalAdmin, project.key)
        if (validationResult.isValid()) {
            def result = archiveProjectService.archiveProject(validationResult)
            return result.isValid()
        }
        return false
    }

    boolean unarchiveProject(@NonNull ApplicationUser globalAdmin, @NonNull Project project) {
        def validationResult = archiveProjectService.validateRestoreProject(globalAdmin, project.key)
        if (validationResult.isValid()) {
            def result = archiveProjectService.restoreProject(validationResult)
            return result.isValid()
        }
        return false
    }

    List<ApplicationUser> adminsForProject(@NonNull Project project, Group... groupsToExcludes) {
        return usersForProjectAndRole(project, ADMIN_PROJECT_ROLE, groupsToExcludes)
    }

    List<ApplicationUser> usersForProjectAndRole(@NonNull Project project, @NonNull ProjectRole projectRole, Group... groupsToExcludes) {
        def users = [] as ArrayList<ApplicationUser>
        def errorCollector = new SimpleErrorCollection()
        def actors = projectRoleService.getProjectRoleActors(projectRole, project, errorCollector)
        actors.getRoleActorsByType(ProjectRoleActor.USER_ROLE_ACTOR_TYPE).each { actor ->
            users.addAll(actor.getUsers())
        }
        actors.getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE).each { actor ->
            def group = ComponentAccessor.groupManager.getGroup(actor.getParameter())
            if (!groupsToExcludes) {
                if (!groupsToExcludes.contains(group)) {
                    users.addAll(actor.getUsers())
                }
            }
        }
        return users
    }

}
package common.jira

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.user.ApplicationUser
import org.springframework.lang.NonNull
import com.atlassian.jira.project.Project
import com.atlassian.jira.jql.builder.JqlQueryBuilder
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.util.SimpleErrorCollection
import com.atlassian.jira.util.ErrorCollection

public class IssueUtils {

    SearchService searchService = ComponentAccessor.getComponent(SearchService)
    IssueManager issueManager = ComponentAccessor.issueManager
    IssueService issueService = ComponentAccessor.issueService

    /*
     * Reassign issues to another user. Useful when an user becomes inactive.
     *
     * @param sourceUser The original assignee
     * @param destinationUser The new assignee
     * @param projects Optional. Limit reassignations to those specific projects
     *
     * @return an error collection
    */
    public ErrorCollection reassignIssues(@NonNull ApplicationUser sourceUser, @NonNull ApplicationUser destinationUser, Project... projects) {
        def errorCollector = new SimpleErrorCollection()
        def queryBuilder = JqlQueryBuilder.newClauseBuilder().assigneeUser(sourceUser.username)
        if (projects) {
            queryBuilder = JqlQueryBuilder.newClauseBuilder(queryBuilder.buildClause()).and().project(projects*.id.toArray(new Long[0]))
        }
        def query = queryBuilder.buildQuery()
        def issues = searchService.search(sourceUser, query, PagerFilter.unlimitedFilter)
        issues.results.each { issue -> 
            def validateResult = issueService.validateAssign(sourceUser, issue.id, destinationUser.username)
            if (validateResult.isValid()) {
                def result = issueService.assign(sourceUser, validateResult)
                if (!result.isValid()) {
                    errorCollector.addErrorCollection(result.errorCollection)
                }
            } else {
                errorCollector.addErrorCollection(validateResult.errorCollection)
            }
        }
        return errorCollector
    }
}
package common.confluence.spaces

import common.confluence.spaces.enums.SpaceStatus
import common.confluence.spaces.enums.SpaceType
import com.atlassian.jira.user.ApplicationUser

class SpaceInfo {
    String name
    String key
    List<ApplicationUser> admins
    String description
    Map homePage
    Integer id
    SpaceStatus status
    SpaceType type
}
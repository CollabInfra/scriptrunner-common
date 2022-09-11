package common.confluence.dc.spaces

import common.confluence.dc.spaces.enums.*
import com.atlassian.jira.user.ApplicationUser

class Space {
    String name
    String key
    List<ApplicationUser> admins
    String description
    Map homePage
    Integer id
    SpaceStatus status
    SpaceType type
}
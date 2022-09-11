package common.confluence.dc.content

import common.confluence.dc.content.enums.*
import common.confluence.dc.spaces.Space
import java.time.LocalDate

class Content {
    Integer id
    ContentType type
    ContentStatus status
    String title
    Space space
    Version version
    String bodyContent
    Integer parentId

    class Version {
        LocalDate when
        Integer number
        String userName
    }

}
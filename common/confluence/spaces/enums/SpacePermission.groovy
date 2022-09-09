package common.confluence.spaces.enums

enum SpacePermission {
    VIEW('VIEWSPACE'),
    EDIT_PAGES('EDITSPACE'),
    EXPORT_PAGE('EXPORTPAGE'),
    MANAGE_PAGE_RESTRICTIONS('SETPAGEPERMISSIONS'),
    REMOVE_PAGES('REMOVEPAGE'),
    EDIT_BLOG_POSTS('EDITBLOG'),
    REMOVE_BLOG_POSTS('REMOVEBLOG'),
    ADD_COMMENTS('COMMENT'),
    REMOVE_OWN_COMMENTS('REMOVECOMMENT'),
    ADD_ATTACHMENTS('CREATEATTACHMENT'),
    REMOVE_ATTACHMENTS('REMOVEATTACHMENT'),
    REMOVE_MAIL('REMOVEMAIL'),
    SPACE_EXPORT('EXPORTSPACE'),
    SPACE_ADMIN('SETSPACEPERMISSIONS')

    private static final Map<String, SpacePermission> BY_PERMISSION = [:]

    static {
        for (SpacePermission e: values()) {
            BY_PERMISSION.put(e.permission, e)
        }
    }

    final String permission

    SpacePermission(String permission) {
        this.permission = permission

    }

    String getValue() {
        return this.permission

    }

    String toString() {
        permission

    }

    public static SpacePermission valueOfPermission(String permission) {
        return BY_PERMISSION.get(permission)
    }

}
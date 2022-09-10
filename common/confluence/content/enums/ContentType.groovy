package common.confluence.content.enums

enum ContentType {
    PAGE('page'),
    BLOG_POST('blogpost')

    private static final Map<String, ContentType> BY_TYPE = [:]

    static {
        for (ContentType e: values()) {
            BY_TYPE.put(e.type, e)
        }
    }

    final String type

    ContentType(String type) {
        this.type = type

    }

    String getValue() {
        return this.type

    }

    String toString() {
        type

    }

    public static ContentType valueOfType(String type) {
        return BY_TYPE.get(type)
    }

}
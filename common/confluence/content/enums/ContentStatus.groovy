package common.confluence.content.enums

enum ContentStatus {
    CURRENT('current'),
    IN_TRASH('trashed'),
    ANY('any')

    private static final Map<String, ContentStatus> BY_STATUS = [:]

    static {
        for (ContentStatus e: values()) {
            BY_STATUS.put(e.status, e)
        }
    }

    final String status

    ContentStatus(String status) {
        this.status = status

    }

    String getValue() {
        return this.status

    }

    String toString() {
        status
    }

    public static ContentStatus valueOfStatus(String status) {
        return BY_STATUS.get(status)
    }

}
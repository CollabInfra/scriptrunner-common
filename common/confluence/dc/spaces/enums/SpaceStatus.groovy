package common.confluence.dc.spaces.enums

enum SpaceStatus {
    CURRENT('CURRENT'),
    ARCHIVED('ARCHIVED')

    private static final Map<String, SpaceStatus> BY_STATUS = [:]

    static {
        for (SpaceStatus e: values()) {
            BY_STATUS.put(e.status, e)
        }
    }

    final String status

    SpaceStatus(String status) {
        this.status = status
    }

    String getValue() {
        return this.status
    }

    String toString() {
        status
    }

    public static SpaceStatus valueOfStatus(String status) {
        return BY_STATUS.get(status)
    }

}
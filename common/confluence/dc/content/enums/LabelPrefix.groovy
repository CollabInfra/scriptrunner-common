package common.confluence.dc.content.enums

enum LabelPrefix {
    GLOBAL('global'),
    MY('my'),
    SYSTEM('system'),
    TEAM('team')

    private static final Map<String, LabelPrefix> BY_PREFIX = [:]

    static {
        for (LabelPrefix e: values()) {
            BY_PREFIX.put(e.prefix, e)
        }
    }

    final String prefix

    LabelPrefix(String prefix) {
        this.prefix = prefix

    }

    String getValue() {
        return this.prefix

    }

    String toString() {
        prefix
    }

    public static LabelPrefix valueOfPrefix(String prefix) {
        return BY_PREFIX.get(prefix)
    }

}
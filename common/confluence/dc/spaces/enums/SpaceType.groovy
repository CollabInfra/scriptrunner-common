package common.confluence.dc.spaces.enums

enum SpaceType {
    GLOBAL('global'),
    PERSONAL('personal')

    private static final Map<String, SpaceType> BY_TYPE = [:]

    static {
        for (SpaceType e: values()) {
            BY_TYPE.put(e.type, e)
        }
    }

    final String type

    SpaceType(String type) {
        this.type = type
    }

    String getType() {
        return this.type
    }

    String toString() {
        type
    }

    public static SpaceType valueOfType(String type) {
        return BY_TYPE.get(type)
    }
}
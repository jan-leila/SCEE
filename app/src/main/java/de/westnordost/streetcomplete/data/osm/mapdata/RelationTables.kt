package de.westnordost.streetcomplete.data.osm.mapdata

object RelationTables {
    const val NAME = "osm_relations"
    const val NAME_MEMBERS = "osm_relation_members"

    object Columns {
        const val ID = "id"
        const val VERSION = "version"
        const val TAGS = "tags"
        const val LAST_UPDATE = "last_update"

        const val INDEX = "idx"
        const val REF = "ref"
        const val TYPE = "type"
        const val ROLE = "role"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} int PRIMARY KEY,
            ${Columns.VERSION} int NOT NULL,
            ${Columns.TAGS} blob,
            ${Columns.LAST_UPDATE} int NOT NULL
        );
    """

    const val MEMBERS_CREATE = """
        CREATE TABLE $NAME_MEMBERS (
            ${Columns.ID} int NOT NULL,
            ${Columns.INDEX} int NOT NULL,
            ${Columns.REF} int NOT NULL,
            ${Columns.TYPE} text NOT NULL,
            ${Columns.ROLE} text NOT NULL
        );
    """

    const val MEMBERS_INDEX_CREATE = """
        CREATE INDEX osm_relation_members_index ON $NAME_MEMBERS (${Columns.ID});
    """
}

package de.westnordost.streetcomplete.data.osm.mapdata

object WayTables {
    const val NAME = "osm_ways"
    const val NAME_NODES = "osm_way_nodes"

    object Columns {
        const val ID = "id"
        const val VERSION = "version"
        const val TAGS = "tags"
        const val LAST_UPDATE = "last_update"

        const val NODE_ID = "node_id"
        const val INDEX = "idx"
    }

    const val CREATE = """
        CREATE TABLE $NAME (
            ${Columns.ID} int PRIMARY KEY,
            ${Columns.VERSION} int NOT NULL,
            ${Columns.TAGS} blob,
            ${Columns.LAST_UPDATE} int NOT NULL
        );
    """

    const val NODES_CREATE = """
        CREATE TABLE $NAME_NODES (
            ${Columns.ID} int NOT NULL,
            ${Columns.INDEX} int NOT NULL,
            ${Columns.NODE_ID} int NOT NULL
        );
    """

    const val NODES_INDEX_CREATE = """
        CREATE INDEX osm_way_nodes_index ON $NAME_NODES (${Columns.ID});
    """
}

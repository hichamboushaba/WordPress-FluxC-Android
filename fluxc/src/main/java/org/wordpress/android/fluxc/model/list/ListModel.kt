package org.wordpress.android.fluxc.model.list

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table

@Table
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE(LOCAL_SITE_ID, TYPE)"
)
class ListModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var dateCreated: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var lastModified: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var localSiteId: Int = 0
    @Column var type: Int? = null
    @Column var filterDbValue: Int? = null
    @Column var orderDbValue: Int? = null
    @Column var stateDbValue: Int = ListState.CAN_LOAD_MORE.value

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    val listDescriptor: ListDescriptor
        get() = ListDescriptor(type, filterDbValue, orderDbValue)

    val state: ListState
        get() = ListState.values().firstOrNull { it.value == this.stateDbValue }!!
}

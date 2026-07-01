package cn.chahuyun.economy.entity.v2.title

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "TitleInfoEntityV2")
@Table(
    name = "hye_title_info",
    indexes = [
        Index(name = "idx_hye_title_user_id", columnList = "user_id"),
        Index(name = "idx_hye_title_code", columnList = "code")
    ]
)
class TitleInfoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "code", length = 128)
    var code: String = "",

    @Column(name = "name", length = 128)
    var name: String = "",

    @Column(name = "status", nullable = false)
    var status: Boolean = false,

    @Column(name = "title", length = 256)
    var title: String = "",

    @Column(name = "impact_name", nullable = false)
    var impactName: Boolean = false,

    @Column(name = "gradient", nullable = false)
    var gradient: Boolean = false,

    @Column(name = "start_color", length = 32)
    var sColor: String = "",

    @Column(name = "end_color", length = 32)
    var eColor: String = "",

    @Column(name = "due_time", nullable = false)
    var dueTime: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable

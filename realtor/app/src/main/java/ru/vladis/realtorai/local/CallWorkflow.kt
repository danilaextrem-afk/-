package ru.vladis.realtorai.local

import androidx.room.withTransaction
import ru.vladis.realtorai.data.*
import ru.vladis.realtorai.hunter.ListingOrigin
import ru.vladis.realtorai.owner.OwnerEvent
import ru.vladis.realtorai.owner.OwnerService
import ru.vladis.realtorai.owner.OwnerStep
import ru.vladis.realtorai.owner.OwnerRules

internal enum class CallOutcome(val title: String) {
    NO_ANSWER("Не дозвонился"), CALLBACK("Просит перезвонить"), MEETING("Согласована встреча"),
    DISCUSS("Обсудили условия"), OPT_OUT("Просит больше не связываться")
}

internal object CallWorkflow {
    data class Changes(val task: TaskItem?, val cancelled: List<Long>)

    fun canDial(property: Property?, phone: String?, people: List<Client>): Boolean {
        val key = ListingOrigin.dialable(phone)?.let(OfflineLogic::phoneKey) ?: return false
        return property?.ownerContactBlocked != true && people.none {
            it.doNotContact && (it.id == property?.ownerId || OfflineLogic.phoneKey(it.phone) == key)
        }
    }

    suspend fun record(
        db: AppDb, propertyId: Long, outcome: CallOutcome, note: String, dueAt: Long?, now: Long
    ): Changes = db.withTransaction {
        val dao = db.dao()
        val property = requireNotNull(dao.property(propertyId)) { "Объект удалён" }
        val owner = property.ownerId?.let { dao.client(it) }
        check(!property.ownerContactBlocked && owner?.doNotContact != true) { "Контакт отказался от дальнейших звонков" }
        val scheduled = outcome == CallOutcome.CALLBACK || outcome == CallOutcome.MEETING
        require(!scheduled || dueAt != null && dueAt > now) { "Укажите будущую дату и время" }
        OwnerService.begin(db, propertyId)
        val text = listOf(outcome.title, note.trim().take(2_000)).filter(String::isNotBlank).joinToString("\n")
        db.owners().event(OwnerEvent(propertyId = propertyId, at = now, kind = "call", text = text))
        if (owner != null) dao.saveInteraction(Interaction(clientId = owner.id, text = text, at = now))

        val kind = "call/$propertyId"
        val active = dao.allTasks().filter { !it.done && (it.kind == kind || it.kind == "meeting/$propertyId") }
        val cancelled = mutableListOf<Long>()
        for (task in active) {
            if (outcome != CallOutcome.OPT_OUT && task.kind.startsWith("meeting/") && outcome != CallOutcome.MEETING) continue
            dao.saveTask(task.copy(done = true))
            cancelled += task.id
        }
        val ownerCase = db.owners().get(propertyId)
        if (ownerCase?.step == OwnerStep.CALL.name && outcome in setOf(CallOutcome.DISCUSS, CallOutcome.MEETING)) {
            db.owners().save(ownerCase.copy(step = OwnerRules.next(OwnerStep.CALL).name))
            for (task in dao.allTasks().filter { !it.done && it.kind == "owner/$propertyId/stage" }) {
                dao.saveTask(task.copy(done = true))
                cancelled += task.id
            }
        }
        if (outcome == CallOutcome.OPT_OUT) {
            dao.saveProperty(property.copy(ownerContactBlocked = true))
            if (owner != null) {
                dao.saveClient(owner.copy(doNotContact = true))
                for (task in dao.allTasks().filter { !it.done && it.kind.startsWith("followup/${owner.id}/") }) {
                    dao.saveTask(task.copy(done = true))
                    cancelled += task.id
                }
            }
        }
        val task = if (scheduled) {
            val item = TaskItem(title = "${if (outcome == CallOutcome.MEETING) "Встреча" else "Перезвонить"} · ${property.title}",
                kind = if (outcome == CallOutcome.MEETING) "meeting/$propertyId" else kind, dueAt = requireNotNull(dueAt))
            item.copy(id = dao.saveTask(item))
        } else null
        Changes(task, cancelled)
    }
}

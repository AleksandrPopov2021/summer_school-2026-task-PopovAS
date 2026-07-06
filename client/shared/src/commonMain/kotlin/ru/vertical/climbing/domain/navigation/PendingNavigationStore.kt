package ru.vertical.climbing.domain.navigation

import ru.vertical.climbing.domain.model.PushNavigationTarget

/** In-memory отложенная навигация до авторизации (LOGIC-013). */
object PendingNavigationStore {
    private var pending: PushNavigationTarget? = null

    fun save(target: PushNavigationTarget) {
        pending = target
    }

    fun consume(): PushNavigationTarget? = pending.also { pending = null }

    fun peek(): PushNavigationTarget? = pending

    fun clear() {
        pending = null
    }
}

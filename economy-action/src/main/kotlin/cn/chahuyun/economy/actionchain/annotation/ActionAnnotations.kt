package cn.chahuyun.economy.actionchain.annotation

/** Marks a suspend object member that should be exposed through a generated rule-chain facade. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Action(val actionId: String)

/** Registers a suspend rule to run before an action. Lower order values run first. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Before(
    val actionId: String,
    val order: Int = 100,
)

/** Registers a suspend rule to run after an action. Lower order values run first. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class After(
    val actionId: String,
    val order: Int = 100,
)

/**
 * Registers a value provider. An empty actionId makes the provider globally visible;
 * otherwise it is visible only to the named action.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Provider(
    val qualifier: String,
    val actionId: String = "",
)

/** Selects a provider qualifier for a rule parameter. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class FromProvider(val qualifier: String = "")

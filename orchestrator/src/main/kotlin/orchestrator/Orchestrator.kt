package technology.idlab.orchestrator

/** The orchestrator handles inter-runner communication and the execution of the pipeline. */
interface Orchestrator {
  /** Execute the pipeline. */
  suspend fun exec()
}

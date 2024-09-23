package technology.idlab.orchestrator

/** The orchestrator handles inter-runner communication and the execution of the pipeline. */
interface Orchestrator {
  /** An indication of the current status of the orchestrator. */
  enum class Status {
    /** The orchestrator has been created, but not initialised. */
    CREATED,
    /** Preparing the orchestrator for execution. */
    INITIALISING,
    /** Ready for execution. */
    READY,
    /** The orchestrator is running. */
    RUNNING,
    /** A failure occurred, and the pipeline has exited. */
    FAILED,
    /** The pipeline has finished. */
    SUCCESS,
  }

  /** The current status of the orchestrator. */
  val status: Status

  /** Execute the pipeline. */
  suspend fun exec()
}

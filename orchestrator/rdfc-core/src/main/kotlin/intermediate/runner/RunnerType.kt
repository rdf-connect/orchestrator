package technology.idlab.rdfc.core.intermediate.runner

/** The types of runner supported by the Orchestrator. */
enum class RunnerType {
  /** A runner which communicates over GRPC. */
  GRPC,
  /** A runner which is built into the Orchestrator. */
  BuiltIn,
}

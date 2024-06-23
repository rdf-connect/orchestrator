import { Server, ServerCredentials } from "@grpc/grpc-js";
import { ServerImplementation } from "./server";
import { RunnerService } from "../proto";

/** The socket at which gRPC binds is decided by the orchestrator. */
const host = process.argv[2];
const port = process.argv[3];

// Initialize the server.
const server = new Server();

// Add the Runner service.
server.addService(RunnerService, new ServerImplementation());

// Startup.
server.bindAsync(
  `${host}:${port}`,
  ServerCredentials.createInsecure(),
  (error, port) => {
    if (error) {
      return console.error(error);
    } else {
      console.log(`gRPC up and running (port=${port})`);
    }
  },
);

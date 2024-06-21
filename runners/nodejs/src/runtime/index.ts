import { Server, ServerCredentials } from "@grpc/grpc-js";
import { ServerImplementation } from "./server";
import { RunnerService } from "../proto";

// Get arguments.
const host = process.argv[2];
const port = process.argv[3];
console.log(`gRPC targeting ${host}:${port}`);

// Initialize the server.
console.log("Initializing server.");
const server = new Server();

// Add the Runner service.
console.log("Adding Runner service.");
server.addService(RunnerService, new ServerImplementation());

// Startup.
console.log("Starting server.");
server.bindAsync(
  `${host}:${port}`,
  ServerCredentials.createInsecure(),
  (error, port) => {
    if (error) {
      return console.error(error);
    } else {
      console.log(`Server started on port ${port}.`);
    }
  },
);

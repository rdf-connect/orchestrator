import { Server, ServerCredentials } from "@grpc/grpc-js";
import { ServerImplementation } from "./server";
import { RunnerService } from "./runner";

// Initialize the server.
const server = new Server();
server.addService(RunnerService, new ServerImplementation());

// Startup.
server.bindAsync(
  "0.0.0.0:50051",
  ServerCredentials.createInsecure(),
  (err, port) => {
    if (err) {
      console.error(err);
    } else {
      console.log(`Server listening on ${port}`);
    }
  },
);

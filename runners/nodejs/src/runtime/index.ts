import { Server, ServerCredentials } from "@grpc/grpc-js";
import { ServerImplementation } from "./server";
import { RunnerService } from "../proto";

// Initialize the server.
const server = new Server();

// Add the Runner service.
server.addService(RunnerService, new ServerImplementation());

// Startup.
server.bindAsync("0.0.0.0:50051", ServerCredentials.createInsecure(), () => {});

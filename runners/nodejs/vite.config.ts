import { defineConfig } from "vitest/config";
import tsconfigPaths from "vite-tsconfig-paths";

defineConfig({
  plugins: [tsconfigPaths()],
});

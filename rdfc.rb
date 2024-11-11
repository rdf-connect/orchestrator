class Rdfc < Formula
  desc "RDC-Connect orchestrator."
  homepage "https://github.com/rdfc-connect/orchestrator"
  url "https://github.com/rdf-connect/orchestrator/releases/download/v0.0.3/rdfc-cli-0.0.3.jar"
  sha256 "0ec119eacda77573063ab7257322e1660115f7e6ee4556029b52303b23168b2f"
  version "0.0.3"
  license "MIT"

  depends_on "openjdk"

  def install
    libexec.install "rdfc-cli-0.0.3.jar"
    (bin/"rdfc").write <<~EOS
      #!/bin/bash
      exec java -jar #{libexec}/rdfc-cli-0.0.3.jar "$@"
    EOS
  end

  test do
    system "#{bin}/rdfc", "--help"
  end
end

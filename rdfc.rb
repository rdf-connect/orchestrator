class Rdfc < Formula
  desc "RDC-Connect orchestrator."
  homepage "https://github.com/rdfc-connect/orchestrator"
  url "https://github.com/rdf-connect/orchestrator/releases/download/v0.0.1/rdfc-orchestrator-0.0.1.jar"
  sha256 "d01643c46a413a10cabf5eb07a53e0c941b4549c21e3203ee760dd1fd90a4e44"
  version "0.0.1"
  license "MIT"

  depends_on "openjdk"

  def install
    libexec.install "rdfc-orchestrator-0.0.1.jar"
    (bin/"rdfc").write <<~EOS
      #!/bin/bash
      exec java -jar #{libexec}/rdfc-orchestrator-0.0.1.jar "$@"
    EOS
  end

  test do
    system "#{bin}/rdfc", "--help"
  end
end

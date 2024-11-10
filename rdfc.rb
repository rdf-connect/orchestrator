class Rdfc < Formula
  desc "RDC-Connect orchestrator."
  homepage "https://github.com/rdfc-connect/orchestrator"
  url "https://github.com/rdf-connect/orchestrator/releases/download/v0.0.2/rdfc-cli-0.0.2.jar"
  sha256 "1195c8fd7d7c8518413e967b8ecbd157f0d12b63648ddb0b63823ab62995738a"
  version "0.0.2"
  license "MIT"

  depends_on "openjdk"

  def install
    libexec.install "rdfc-cli-0.0.2.jar"
    (bin/"rdfc").write <<~EOS
      #!/bin/bash
      exec java -jar #{libexec}/rdfc-cli-0.0.2.jar "$@"
    EOS
  end

  test do
    system "#{bin}/rdfc", "--help"
  end
end

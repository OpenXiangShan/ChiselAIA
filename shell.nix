let
  name = "ChiselAIA";
  # pin nixpkgs to nixos-24.05 (cocotb 1.x)
  pkgs = import (fetchTarball {
    url = "https://github.com/NixOS/nixpkgs/archive/ecbc1ca8ffd6aea8372ad16be9ebbb39889e55b6.tar.gz";
    sha256 = "0yfaybsa30zx4bm900hgn3hz92javlf4d47ahdaxj9fai00ddc1x";
  }) {};
  my-python3 = pkgs.python3.withPackages (python-pkgs: [
    python-pkgs.cocotb
    # for docs
    python-pkgs.pydot
  ]);
  h_content = builtins.toFile "h_content" ''
    # ${pkgs.lib.toUpper "${name} usage tips"}

    * Show this help: `h`
    * Enter nix-shell: `nix-shell` (`direnv` recommanded!)
    * Before running, make sure git submodules have been updated.
      * `git submodule update --init --recursive`
    * Run Unit Tests: `make -j`
      * The tilelink verilog is generated into `gen/` folder.
      * The axi4 verilog is generated into `gen_axi/` folder.
      * Run a single unit test: `make run-aplic`, `make run-imsic`, ...
        * The available unit tests are located in test/*/main.py
  '';
  _h_ = pkgs.writeShellScriptBin "h" ''
    ${pkgs.glow}/bin/glow ${h_content}
  '';
  markcode = pkgs.callPackage (pkgs.fetchFromGitHub {
    owner = "xieby1";
    repo = "markcode";
    rev = "bec9fa8279a23e387825b2a79b66ec77ed52220c";
    hash = "sha256-VzqERMEs/8dOz3n4YfNADLrdA2keqxUek62tqID9TnM=";
  }){};
  # Mill standalone launcher that reads .mill-version (pinned to 0.12.17) and
  # bootstraps the matching Mill binary into ~/.cache/mill/download.
  mill-launcher = pkgs.runCommand "mill-launcher" {
    nativeBuildInputs = [ pkgs.makeWrapper ];
  } ''
    install -Dm755 ${pkgs.fetchurl {
      url = "https://raw.githubusercontent.com/com-lihaoyi/mill/0.12.17/mill";
      sha256 = "1bx2427ifirg1k2n150b1z9wddf5d74k31vh8j039h3004vwk6h9";
    }} $out/bin/mill
    wrapProgram $out/bin/mill --prefix PATH : ${pkgs.lib.makeBinPath [ pkgs.curl pkgs.coreutils pkgs.findutils pkgs.gnugrep pkgs.gnused ]}
  '';
in pkgs.mkShell {
  inherit name;

  buildInputs = [
    _h_
    mill-launcher
    pkgs.jdk
    pkgs.verilator
    pkgs.gtkwave
    my-python3
    # for generating gtkwave's fst waveform
    pkgs.zlib
    # for docs
    pkgs.graphviz
    pkgs.mdbook
    pkgs.drawio-headless
    markcode
  ];

  shellHook = ''
    # Pin mill version for the standalone launcher (also inherited by server subprocesses)
    export MILL_VERSION="''${MILL_VERSION:-$(cat .mill-version 2>/dev/null)}"
    export PYTHONPATH+=:${my-python3}/lib/${my-python3.libPrefix}/site-packages
    export PYTHONPATH+=:$(realpath ./test)
    export LIBGL_ALWAYS_SOFTWARE=1
    # To enable pdb when cocotb test failed
    export COCOTB_PDB_ON_EXCEPTION=1
    h
  '';
}

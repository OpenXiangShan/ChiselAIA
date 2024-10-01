let
  name = "OpenAIA.scala";
  pkgs = import <nixpkgs> {};
  my-python3 = pkgs.python3.withPackages (python-pkgs: [
    python-pkgs.cocotb
  ]);
  h_content = builtins.toFile "h_content" ''
    # ${pkgs.lib.toUpper "${name} usage tips"}

    * Show this help: `h`
    * Enter nix-shell: `nix-shell` (`direnv` recommanded!)
    * Generate verilog: `mill OpenAIA`
    * Run unit tests: `make -C test`
  '';
  _h_ = pkgs.writeShellScriptBin "h" ''
    ${pkgs.glow}/bin/glow ${h_content}
  '';
in pkgs.mkShell {
  inherit name;

  buildInputs = [
    _h_
    pkgs.mill
    pkgs.verilator
    pkgs.gtkwave
    my-python3
    # for generating gtkwave's fst waveform
    pkgs.zlib
  ];

  shellHook = let
    circt_1_62_0 = (import (pkgs.fetchFromGitHub {
      owner = "NixOS";
      repo = "nixpkgs";
      rev = "771b079bb84ac2395f3a24a5663ac8d1495c98d3";
      sha256 = "0l1l9ms78xd41xg768pkb6xym200zpf4zjbv4kbqbj3z7rzvhpb7";
    }){}).circt;
  in ''
    export CHISEL_FIRTOOL_PATH=${circt_1_62_0}/bin/
    export PYTHONPATH+=${my-python3}/lib/${my-python3.libPrefix}/site-packages
    h
  '';
}

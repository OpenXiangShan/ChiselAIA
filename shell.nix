let
  name = "OpenAIA.scala";
  pkgs = import <nixpkgs> {};
in pkgs.mkShell {
  inherit name;

  buildInputs = [
    pkgs.mill
    pkgs.verilator
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
  '';
}

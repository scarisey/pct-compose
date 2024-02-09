{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    scarisey-dotfiles.url = "github:scarisey/nixos-dotfiles";
  };
  outputs = { self, nixpkgs, flake-utils, scarisey-dotfiles }: flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = import nixpkgs {
        inherit system;
        overlays = [
          (final: prev: {
            jdk = final.graalvmCEPackages.graalvm-ce-musl;
            jre = final.graalvmCEPackages.graalvm-ce-musl;
          })
          scarisey-dotfiles.overlays.additions
        ];
      };
      stubb = {
        mkdir = pkgs.writeScriptBin "mkdir" ''
          echo $@
        '';
        chown = pkgs.writeScriptBin "chown" ''
          echo $@
        '';
        chmod = pkgs.writeScriptBin "chmod" ''
          echo $@
        '';
        pct = pkgs.writeScriptBin "pct" ''
          echo $@
        '';
        iptables = pkgs.writeScriptBin "iptables" ''
          echo $@
        '';
        iptables-save = pkgs.writeScriptBin "iptables-save" ''
          echo $@
        '';
      };
    in
    with pkgs;
    {
      devShells.default = pkgs.mkShell {
        LD_LIBRARY_PATH = "${pkgs.stdenv.cc.cc.lib}/lib";
        packages = with pkgs; [ sbt scalafmt graalvmCEPackages.graalvm-ce-musl antora ] ++ (map (p: stubb.${p}) (builtins.attrNames stubb));
        shellHook = ''
          export JAVA_HOME=${pkgs.graalvmCEPackages.graalvm-ce-musl}
          export NATIVE_IMAGE_INSTALLED=true
        '';
      };
    }
  );
}

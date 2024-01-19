{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = { self, nixpkgs, flake-utils }: flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = import nixpkgs {
        inherit system;
        overlays = [
          (final: prev: {
            jdk = final.graalvmCEPackages.graalvm-ce-musl;
            jre = final.graalvmCEPackages.graalvm-ce-musl;
          })
        ];
      };
    in
    with pkgs;
    {
      devShells.default = pkgs.mkShell {
        LD_LIBRARY_PATH = "${pkgs.stdenv.cc.cc.lib}/lib";
        packages = with pkgs; [ sbt scalafmt graalvmCEPackages.graalvm-ce-musl ];
        shellHook = ''
          export JAVA_HOME=${pkgs.graalvmCEPackages.graalvm-ce-musl}
          export NATIVE_IMAGE_INSTALLED=true
        '';
      };
    }
  );
}

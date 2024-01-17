{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = { self, nixpkgs, flake-utils }: flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = import nixpkgs { inherit system; };
    in
    {
      devShells.default = pkgs.mkShell {
        packages = with pkgs; [ sbt scalafmt graalvmCEPackages.graalvm-ce-musl ];
        shellHook = ''
          export JAVA_HOME=${pkgs.graalvmCEPackages.graalvm-ce-musl}
        '';
      };
    }
  );
}

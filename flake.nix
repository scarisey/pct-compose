{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-23.11";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = { self, nixpkgs, flake-utils }: flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = nixpkgs.legacyPackages.${system};
    in
    with pkgs;
    {
      devShells.default = mkShell {
        packages = [ pkgs.coursier pkgs.scalafmt pkgs.graalvm-ce ];
        shellHook = ''
          export JAVA_HOME=${pkgs.graalvm-ce}/lib/openjdk
        '';
      };
    }
  );
}

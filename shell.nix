{ pkgs ? import <nixpkgs> { } }:
pkgs.mkShell {
  packages = [ pkgs.coursier pkgs.scalafmt pkgs.openjdk17 ];
  shellHook = ''
    export JAVA_HOME=${pkgs.openjdk17}
  '';
}

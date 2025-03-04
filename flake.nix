{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      pkgs = nixpkgs.legacyPackages.x86_64-linux.pkgs;
    in
    {
      devShells.x86_64-linux.default = pkgs.mkShell {
        buildInputs = [
          (pkgs.clojure.override { jdk = pkgs.zulu; })
          pkgs.leiningen
          pkgs.zulu
          pkgs.clj-kondo
          pkgs.nodejs_18
          pkgs.yarn
        ];
      };
    };
}
